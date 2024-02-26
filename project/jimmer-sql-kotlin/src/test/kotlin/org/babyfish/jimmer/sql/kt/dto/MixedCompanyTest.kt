package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.common.assertContentEquals
import org.babyfish.jimmer.sql.kt.model.flat.Company
import org.babyfish.jimmer.sql.kt.model.flat.by
import org.babyfish.jimmer.sql.kt.model.flat.dto.MixedCompanyView
import org.junit.Test

class MixedCompanyTest {

    @Test
    fun testDtoToEntity() {
        val view = MixedCompanyView(
            id = 0L,
            companyName = "myCompany",
            streetName = "myStreet",
            cityName = "myCity",
            province = MixedCompanyView.TargetOf_province(
                provinceName = "myProvince",
                country = MixedCompanyView.TargetOf_province.TargetOf_country_2(
                    countryName = "myCountry"
                )
            )
        )
        assertContentEquals(
            """{
                |--->"id":0,
                |--->"companyName":"myCompany",
                |--->"street":{
                |--->--->"streetName":"myStreet",
                |--->--->"city":{
                |--->--->--->"cityName":"myCity",
                |--->--->--->"province":{
                |--->--->--->--->"provinceName":"myProvince",
                |--->--->--->--->"country":{
                |--->--->--->--->--->"countryName":"myCountry"
                |--->--->--->--->}
                |--->--->--->}
                |--->--->}
                |--->}
                |}""".trimMargin(),
            view.toEntity()
        )
    }

    @Test
    fun testEntityToDto() {
        val company = new(Company::class).by {
            id = 1L
            companyName = "myCompany"
            street().apply {
                streetName = "myStreet"
                city().apply {
                    cityName = "myCity"
                    province().apply {
                        provinceName = "myProvince"
                        country().apply {
                            countryName = "myCountry"
                        }
                    }
                }
            }
        }
        assertContentEquals(
            """MixedCompanyView(
                |--->id=1, 
                |--->companyName=myCompany, 
                |--->value=null, 
                |--->streetName=myStreet, 
                |--->cityName=myCity, 
                |--->province=MixedCompanyView.TargetOf_province(
                |--->--->provinceName=myProvince, 
                |--->--->country=MixedCompanyView.TargetOf_province.TargetOf_country_2(
                |--->--->--->countryName=myCountry
                |--->--->)
                |--->)
                |)""".trimMargin(),
            MixedCompanyView(company)
        )
    }
}