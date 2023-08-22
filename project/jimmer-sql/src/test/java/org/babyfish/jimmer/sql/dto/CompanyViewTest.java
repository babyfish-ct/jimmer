package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.flat.Company;
import org.babyfish.jimmer.sql.model.flat.CompanyDraft;
import org.babyfish.jimmer.sql.model.flat.dto.CompanyView;
import org.junit.jupiter.api.Test;

public class CompanyViewTest extends Tests {

    @Test
    public void testDtoToEntity() {

        CompanyView view = new CompanyView();
        view.setCountryName("myCountry");
        view.setProvinceName("myProvince");
        view.setCityName("myCity");
        view.setStreetName("myStreet");
        view.setCompanyName("myCompany");

        assertContentEquals(
                "{" +
                        "--->\"companyName\":\"myCompany\"," +
                        "--->\"street\":{" +
                        "--->--->\"streetName\":\"myStreet\"," +
                        "--->--->\"city\":{" +
                        "--->--->--->\"cityName\":\"myCity\"," +
                        "--->--->--->\"province\":{" +
                        "--->--->--->--->\"provinceName\":\"myProvince\"," +
                        "--->--->--->--->\"country\":{" +
                        "--->--->--->--->--->\"countryName\":\"myCountry\"" +
                        "--->--->--->--->}" +
                        "--->--->--->}" +
                        "--->--->}" +
                        "--->}" +
                        "}",
                view.toEntity()
        );
    }

    @Test
    public void testEntityToDto() {

        Company company = CompanyDraft.$.produce(draft -> {
            draft.setCompanyName("myCompany");
            draft.applyStreet(street -> {
                street.setStreetName("myStreet");
                street.applyCity(city -> {
                    city.setCityName("myCity");
                    city.applyProvince(province -> {
                        province.setProvinceName("myProvince");
                        province.applyCountry(country -> {
                            country.setCountryName("myCountry");
                        });
                    });
                });
            });
        });

        assertContentEquals(
                "CompanyView(" +
                        "--->id=0, " +
                        "--->companyName=myCompany, " +
                        "--->streetName=myStreet, " +
                        "--->cityName=myCity, " +
                        "--->provinceName=myProvince, " +
                        "--->countryName=myCountry, " +
                        "--->tag=0" +
                        ")",
                new CompanyView(company)
        );
    }
}
