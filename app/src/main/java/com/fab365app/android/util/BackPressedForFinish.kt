package com.app.uxis.lf.util

import android.app.Activity
import android.widget.Toast

class BackPressedForFinish(private val activity: Activity) {
    private var backKeyPressedTime: Long = 0    //'뒤로' 버튼을 클릭 했을 띠의 시간
    private var toast: Toast? = null    // 종료 안내 문구 Toast
    private val TIME_INTERVAL: Long = 2000  // 첫번째 버튼 클릭과 두번째 버튼 클릭 사이의 종료를 위한 시간차를 정의

    fun onBackPressed() {
        // '뒤로' 버튼 클릭 시간과 현재 시간을 비교 게산한다.

        // 마지막 '뒤로'버튼 클릭 시간이 이전 '뒤로'버튼 클릭시간과의 차이가 TIME_INTERVAL(여기서는 2초)보다 클 때 true
        if (System.currentTimeMillis() > backKeyPressedTime + TIME_INTERVAL) {

            // 현재 시간을 backKeyPressedTime에 저장한다.
            backKeyPressedTime = System.currentTimeMillis()

            // 종료 안내문구를 노출한다.
            showMessage()
        } else {
            // 마지막 '뒤로'버튼 클릭시간이 이전 '뒤로'버튼 클릭시간과의 차이가 TIME_INTERVAL(2초)보다 작을때

            // Toast가 아직 노출중이라면 취소한다.
            toast?.cancel()
            activity.finish()
            System.exit(0)
        }
    }

    private fun showMessage() {
        toast = Toast.makeText(activity, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT)
        toast?.show()
    }
}