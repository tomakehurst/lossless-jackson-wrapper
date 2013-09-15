package com.github.tomakehurst.losslessjackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ImmutableContactDetails {

    private final String homePhone;
    private final String email;

    @JsonCreator
    public ImmutableContactDetails(@JsonProperty("homePhone") String homePhone, @JsonProperty("email") String email) {
        this.homePhone = homePhone;
        this.email = email;
    }

    public String getHomePhone() {
        return homePhone;
    }

    public String getEmail() {
        return email;
    }
}
