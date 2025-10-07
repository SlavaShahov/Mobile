package com.example.myapplication.data.model

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import org.simpleframework.xml.Path

@Root(name = "Record", strict = false)
data class GoldRateRecord(
    @field:Attribute(name = "Date", required = false)
    var date: String = "",

    @field:Attribute(name = "Code", required = false)
    var code: String = "",

    @field:Element(name = "Buy", required = false)
    var buy: String = "",

    @field:Element(name = "Sell", required = false)
    var sell: String = ""
)

@Root(name = "Metall", strict = false)
data class GoldRatesResponse(
    @field:ElementList(inline = true, entry = "Record", required = false)
    var records: MutableList<GoldRateRecord> = mutableListOf()
)