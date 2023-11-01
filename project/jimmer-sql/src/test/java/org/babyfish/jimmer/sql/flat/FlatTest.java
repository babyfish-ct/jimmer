package org.babyfish.jimmer.sql.flat;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.impl.util.DtoPropAccessor;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.flat.*;
import org.junit.jupiter.api.Test;

public class FlatTest extends Tests {

    @Test
    public void testChildToParent() {

        Company company = CompanyDraft.$.produce(draft -> {
            set(
                    draft, 
                    new int[] {
                        CompanyDraft.Producer.SLOT_COMPANY_NAME
                    },
                    "myCompany"
            );
        });
        assertContentEquals(
                "{" +
                        "--->\"companyName\":\"myCompany\"" +
                        "}",
                company
        );

        company = CompanyDraft.$.produce(company, draft -> {
            set(
                    draft,
                    new int[] {
                            CompanyDraft.Producer.SLOT_STREET,
                            StreetDraft.Producer.SLOT_STREET_NAME
                    },
                    "myStreet"
            );
        });
        assertContentEquals(
                "{" +
                        "--->\"companyName\":\"myCompany\"," +
                        "--->\"street\":{" +
                        "--->--->\"streetName\":\"myStreet\"" +
                        "--->}" +
                        "}",
                company
        );

        company = CompanyDraft.$.produce(company, draft -> {
            set(
                    draft,
                    new int[] {
                            CompanyDraft.Producer.SLOT_STREET,
                            StreetDraft.Producer.SLOT_CITY,
                            CityDraft.Producer.SLOT_CITY_NAME
                    },
                    "myCity"
            );
        });
        assertContentEquals(
                "{" +
                        "--->\"companyName\":\"myCompany\"," +
                        "--->\"street\":{" +
                        "--->--->\"streetName\":\"myStreet\"," +
                        "--->--->\"city\":{" +
                        "--->--->--->\"cityName\":\"myCity\"" +
                        "--->--->}" +
                        "--->}" +
                        "}",
                company
        );

        company = CompanyDraft.$.produce(company, draft -> {
            set(
                    draft,
                    new int[] {
                            CompanyDraft.Producer.SLOT_STREET,
                            StreetDraft.Producer.SLOT_CITY,
                            CityDraft.Producer.SLOT_PROVINCE,
                            ProvinceDraft.Producer.SLOT_PROVINCE_NAME
                    },
                    "myProvince"
            );
        });
        assertContentEquals(
                "{" +
                        "--->\"companyName\":\"myCompany\"," +
                        "--->\"street\":{" +
                        "--->--->\"streetName\":\"myStreet\"," +
                        "--->--->\"city\":{" +
                        "--->--->--->\"cityName\":\"myCity\"," +
                        "--->--->--->\"province\":{" +
                        "--->--->--->--->\"provinceName\":\"myProvince\"" +
                        "--->--->--->}" +
                        "--->--->}" +
                        "--->}" +
                        "}",
                company
        );

        company = CompanyDraft.$.produce(company, draft -> {
            set(
                    draft,
                    new int[] {
                            CompanyDraft.Producer.SLOT_STREET,
                            StreetDraft.Producer.SLOT_CITY,
                            CityDraft.Producer.SLOT_PROVINCE,
                            ProvinceDraft.Producer.SLOT_COUNTRY,
                            CountryDraft.Producer.SLOT_COUNTRY_NAME
                    },
                    "myCountry"
            );
        });
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
                        "--->--->--->--->--->\"countryName\":\"myCountry\"}" +
                        "--->--->--->}" +
                        "--->--->}" +
                        "--->}" +
                        "}",
                company
        );
    }

    @Test
    public void testParentToChild() {

        Company company = CompanyDraft.$.produce(draft -> {
            set(
                    draft,
                    new int[] {
                            CompanyDraft.Producer.SLOT_STREET,
                            StreetDraft.Producer.SLOT_CITY,
                            CityDraft.Producer.SLOT_PROVINCE,
                            ProvinceDraft.Producer.SLOT_COUNTRY,
                            CountryDraft.Producer.SLOT_COUNTRY_NAME
                    },
                    "myCountry"
            );
        });
        assertContentEquals(
                "{" +
                        "--->\"street\":{" +
                        "--->--->\"city\":{" +
                        "--->--->--->\"province\":{" +
                        "--->--->--->--->\"country\":{" +
                        "--->--->--->--->--->\"countryName\":\"myCountry\"" +
                        "--->--->--->--->}" +
                        "--->--->--->}" +
                        "--->--->}" +
                        "--->}" +
                        "}",
                company
        );

        company = CompanyDraft.$.produce(company, draft -> {
            set(
                    draft,
                    new int[] {
                            CompanyDraft.Producer.SLOT_STREET,
                            StreetDraft.Producer.SLOT_CITY,
                            CityDraft.Producer.SLOT_PROVINCE,
                            ProvinceDraft.Producer.SLOT_PROVINCE_NAME
                    },
                    "myProvince"
            );
        });
        assertContentEquals(
                "{" +
                        "--->\"street\":{" +
                        "--->--->\"city\":{" +
                        "--->--->--->\"province\":{" +
                        "--->--->--->--->\"provinceName\":\"myProvince\"," +
                        "--->--->--->--->\"country\":{" +
                        "--->--->--->--->--->\"countryName\":\"myCountry\"" +
                        "--->--->--->--->}" +
                        "--->--->--->}" +
                        "--->--->}" +
                        "--->}" +
                        "}",
                company
        );

        company = CompanyDraft.$.produce(company, draft -> {
            set(
                    draft,
                    new int[] {
                            CompanyDraft.Producer.SLOT_STREET,
                            StreetDraft.Producer.SLOT_CITY,
                            CityDraft.Producer.SLOT_CITY_NAME,
                    },
                    "myCity"
            );
        });
        assertContentEquals(
                "{" +
                        "--->\"street\":{" +
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
                company
        );

        company = CompanyDraft.$.produce(company, draft -> {
            set(
                    draft,
                    new int[] {
                            CompanyDraft.Producer.SLOT_STREET,
                            StreetDraft.Producer.SLOT_STREET_NAME,
                    },
                    "myStreet"
            );
        });
        assertContentEquals(
                "{" +
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
                company
        );

        company = CompanyDraft.$.produce(company, draft -> {
            set(
                    draft,
                    new int[] {
                            CompanyDraft.Producer.SLOT_COMPANY_NAME,
                    },
                    "myCompany"
            );
        });
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
                company
        );
    }

    private static void set(Draft draft, int[] propIds, Object value) {
        new DtoPropAccessor(false, propIds).set(draft, value);
    }
}
