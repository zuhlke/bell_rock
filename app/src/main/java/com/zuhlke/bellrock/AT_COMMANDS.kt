package com.zuhlke.bellrock


class AT_COMMANDS {
    companion object {
        const val TEXT_MODE = "AT+CMGF=1\r"
        const val READ_SMS = "AT+CMGR=1\r"
        const val CLEAR_SMS = "AT+CMGDA=\"DEL ALL\"\r"
    }
}

class AT_RESPONSES {
    companion object {
        const val OK = "OK"
        const val TEXT_MODE = "AT+CMGF=1"
        const val READ_SMS = "+CMGR"
        const val CLEAR_SMS = "AT+CMGDA=\"DEL ALL\""
    }
}