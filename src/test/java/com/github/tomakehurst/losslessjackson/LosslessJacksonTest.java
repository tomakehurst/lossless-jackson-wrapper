package com.github.tomakehurst.losslessjackson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static net.sf.json.test.JSONAssert.assertJsonEquals;

public class LosslessJacksonTest {

    private ObjectMapper objectMapper;

    @Before
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    static final String CONTACT_DETAILS_DOCUMENT =
        "{                                  \n" +
        " \"homePhone\": \"01234 567890\",  \n" +
        " \"mobilePhone\": \"07123 123456\",\n" +
        " \"email\": \"someone@email.com\", \n" +
        " \"address\": {                    \n" +
        "  \"line1\": \"1 Toad Road\",      \n" +
        "  \"city\": \"London\",            \n" +
        "  \"postcode\": \"E1 1TD\"         \n" +
        " }                                 \n" +
        "}";

    @Test
    public void preservesAdditionalAttributesForImmutableSingleConstructorValueClass() throws Exception {
        assertSerialisedMatchesOriginalDocument(
                CONTACT_DETAILS_DOCUMENT,
                ImmutableContactDetails.class,
                LosslessJackson.lossless(ImmutableContactDetails.class));
    }

    private <T> void assertSerialisedMatchesOriginalDocument(String originalDocument, Class<T> sourceClass, Class<? extends T> generatedClass) throws IOException {
        T deserialisedObject = objectMapper.readValue(originalDocument, generatedClass);
        String reserialisedDocument = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deserialisedObject);
        assertJsonEquals("\nExpected:\n" + originalDocument + "\n\nActual:\n" + reserialisedDocument + "\n\n",
                originalDocument, reserialisedDocument);
    }
}
