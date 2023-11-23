package com.example.vtubercamera.multiFragment

interface BackButtonCallBack {
    fun onBackButtonPressed()
}

class MultiFragmentPresenter {
    private var backButtonCallBack: BackButtonCallBack? = null
    fun setBackButtonCallBack(callBack: BackButtonCallBack) {
        backButtonCallBack = callBack
    }

    fun onBackButtonPressed() {
        backButtonCallBack?.onBackButtonPressed()
    }
}