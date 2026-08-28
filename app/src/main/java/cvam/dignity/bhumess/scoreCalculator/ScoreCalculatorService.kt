package cvam.dignity.bhumess

import android.content.Context
import cvam.dignity.bhumess.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

class ScoreCalculatorService(private val context: Context) {
    private val client = OkHttpClient.Builder().followRedirects(true).build()
    private val prefs = context.getSharedPreferences("bhuji_score_history_v2", Context.MODE_PRIVATE)

    suspend fun evaluateUrl(url: String, subjectsJson: JSONObject): ScoreEvaluationReport = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            val res = client.newCall(req).execute()
            val html = res.body?.string() ?: ""
            if (html.isEmpty()) return@withContext ScoreEvaluationReport(score = -404)

            val doc = Jsoup.parse(html)
            val fullText = doc.text().replace("\\s+".toRegex(), "")
            var matchedKey: String? = null
            var finalSubjectDisplayName = ""

            val jsonKeys = subjectsJson.keys()
            while(jsonKeys.hasNext()) {
                val k = jsonKeys.next() as String
                val targetName = subjectsJson.getJSONObject(k).getString("name")
                if (fullText.lowercase().contains("subject${targetName.replace("\\s+".toRegex(), "").lowercase()}")) {
                    matchedKey = k
                    finalSubjectDisplayName = targetName
                    break
                }
            }

            if (matchedKey == null) return@withContext ScoreEvaluationReport(subjectName = "Unknown", score = -999)

            val report = processContent(doc, finalSubjectDisplayName, subjectsJson.getJSONObject(matchedKey).optJSONObject("key"))
            if (report.questions.isNotEmpty()) saveToHistory(report)
            report
        } catch (e: Exception) { ScoreEvaluationReport(score = -404) }
    }

    private fun processContent(doc: org.jsoup.nodes.Document, subject: String, keys: JSONObject?): ScoreEvaluationReport {
        val elements = doc.select(".questionPanel, .question-pnl, table.questionTable")
        val results = mutableListOf<ScoreQuestionResult>()

        elements.forEachIndexed { i, el ->
            val text = el.text()
            val qId = Regex("Question ID\\s*[:\\s]*(\\d+)").find(text)?.groupValues?.get(1) ?: ""
            val opts = (1..4).map { Regex("Option $it ID\\s*[:\\s]*(\\d+)").find(text)?.groupValues?.get(1) ?: "" }
            val chosen = Regex("Chosen Option\\s*[:\\s]*(\\d+)").find(text)?.groupValues?.get(1) ?: ""
            val selId = when(chosen) { "1" -> opts.getOrNull(0) ?: ""; "2" -> opts.getOrNull(1) ?: ""; "3" -> opts.getOrNull(2) ?: ""; "4" -> opts.getOrNull(3) ?: ""; else -> "" }
            val correctId = keys?.optString(qId, "") ?: ""

            val status = when {
                correctId.equals("Dropped", true) -> ScoreEvaluationStatus.DROPPED
                selId.isEmpty() -> ScoreEvaluationStatus.SKIPPED
                correctId.isEmpty() -> ScoreEvaluationStatus.KEY_MISSING
                correctId.split(",").contains(selId) -> ScoreEvaluationStatus.CORRECT
                else -> ScoreEvaluationStatus.WRONG
            }
            results.add(ScoreQuestionResult(i + 1, qId, selId, correctId, status))
        }

        val c = results.count { it.status == ScoreEvaluationStatus.CORRECT || it.status == ScoreEvaluationStatus.DROPPED }
        val w = results.count { it.status == ScoreEvaluationStatus.WRONG }
        return ScoreEvaluationReport(subject, (c * 4) - w, c, w, results.count { it.status == ScoreEvaluationStatus.SKIPPED }, System.currentTimeMillis(), results)
    }

    fun saveToHistory(report: ScoreEvaluationReport) {
        val history = getHistory().toMutableList()
        history.add(0, report)
        writeHistory(history.distinctBy { it.timestamp }.take(10))
    }

    fun deleteHistoryItem(report: ScoreEvaluationReport) {
        writeHistory(getHistory().filterNot { it.timestamp == report.timestamp })
    }

    private fun writeHistory(list: List<ScoreEvaluationReport>) {
        val array = JSONArray()
        list.forEach { rep ->
            val obj = JSONObject().apply {
                put("sn", rep.subjectName); put("sc", rep.score); put("cc", rep.correctCount)
                put("wc", rep.wrongCount); put("sk", rep.skippedCount); put("ts", rep.timestamp)
                val qArr = JSONArray()
                rep.questions.forEach { q ->
                    qArr.put(JSONObject().apply { put("i", q.index); put("qi", q.qId); put("ci", q.chosenOptionId); put("ai", q.correctAnswerId); put("st", q.status.name) })
                }
                put("qs", qArr)
            }
            array.put(obj)
        }
        prefs.edit().putString("score_history", array.toString()).apply()
    }

    fun getHistory(): List<ScoreEvaluationReport> {
        val raw = prefs.getString("score_history", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<ScoreEvaluationReport>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val qArr = o.getJSONArray("qs")
            val qs = mutableListOf<ScoreQuestionResult>()
            for (j in 0 until qArr.length()) {
                val q = qArr.getJSONObject(j)
                qs.add(ScoreQuestionResult(q.getInt("i"), q.getString("qi"), q.getString("ci"), q.getString("ai"), ScoreEvaluationStatus.valueOf(q.getString("st"))))
            }
            list.add(ScoreEvaluationReport(o.getString("sn"), o.getInt("sc"), o.getInt("cc"), o.getInt("wc"), o.getInt("sk"), o.getLong("ts"), qs))
        }
        return list
    }
}

