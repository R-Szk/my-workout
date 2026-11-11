package com.myworkout.data

object HomeCatalog {
    fun isBodyweight(id: String) = id == "push_up" || id == "squat"
    fun initialReps(id: String) = if (id == "lateral_raise" || id == "squat") 10 else 8
    fun notes(id: String): String = when(id) {
        "push_up" -> "体を一直線に保ち、ゆっくり胸を下ろしましょう。"
        "lateral_raise" -> "反動を使わず、腕を肩の高さまで上げましょう。"
        "triceps_extension" -> "ダンベル1個を両手で持ち、肘の位置を保って上げ下げしましょう。"
        "one_arm_row" -> "左右それぞれ同じ回数を行い、両側が終わったらセット完了。"
        "dumbbell_curl" -> "肘を体の横に保ち、ゆっくり上げ下げしましょう。"
        else -> "背中を丸めず、無理のない深さでしゃがみましょう。"
    }
}
