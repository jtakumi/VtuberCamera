package com.example.vtubercamera.multiFragment

class NavigationTracker {
    companion object {
        private var previousActivity: Class<*>? = null

        fun setPreviousActivity(activityClass: Class<*>) {
            previousActivity = activityClass
        }

        fun getPreviousActivity(): Class<*>? {
            return previousActivity
        }
    }
}