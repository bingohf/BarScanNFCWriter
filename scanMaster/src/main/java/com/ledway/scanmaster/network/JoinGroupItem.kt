package com.ledway.scanmaster.network


data class JoinGroupItem(val myTaxNo:String,
    val line:String,
    val mt_server:String?,val mt_port:String?,val mt_company:String?,
    val sm_server:String?,val sm_port:Int?,val sm_company:String?,
    val se_server:String?,val se_port:Int?,val se_company:String?
)