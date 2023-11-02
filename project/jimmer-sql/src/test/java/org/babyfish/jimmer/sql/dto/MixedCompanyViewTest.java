package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.flat.Company;
import org.babyfish.jimmer.sql.model.flat.CompanyDraft;
import org.babyfish.jimmer.sql.model.flat.dto.MixedCompanyView;
import org.junit.jupiter.api.Test;

public class MixedCompanyViewTest extends Tests {

    @Test
    public void testDtoToEntity() {
        MixedCompanyView view = new MixedCompanyView();
        view.setId(1L);
        view.setCompanyName("myCompany");
        view.setStreetName("myStreet");
        view.setCityName("myCity");

        MixedCompanyView.TargetOf_province province = new MixedCompanyView.TargetOf_province();
        province.setProvinceName("myProvince");
        view.setProvince(province);

        MixedCompanyView.TargetOf_province.TargetOf_country_2 country =
                new MixedCompanyView.TargetOf_province.TargetOf_country_2();
        country.setCountryName("myCountry");
        province.setCountry(country);

        assertContentEquals(
                "{" +
                        "--->\"id\":1," +
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
            draft.setId(1L);
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
                "MixedCompanyView(" +
                        "--->id=1, " +
                        "--->companyName=myCompany, " +
                        "--->streetName=myStreet, " +
                        "--->cityName=myCity, " +
                        "--->province=MixedCompanyView.TargetOf_province(" +
                        "--->--->provinceName=myProvince, " +
                        "--->--->country=MixedCompanyView.TargetOf_province.TargetOf_country_2(" +
                        "--->--->--->countryName=myCountry" +
                        "--->--->)" +
                        "--->)" +
                        ")",
                new MixedCompanyView(company)
        );
    }
}
