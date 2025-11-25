package com.quip.coa.utilities;

import org.springframework.stereotype.Component;

@Component
public class Utility {
    public String getClientName(String domain) {
        return domain.substring(8).split("\\.")[0].concat(Constants.CLIENT_AUTHOR_FIELD);
    }

    public String getDomainName(String domain) {
        return domain.substring(8).split("\\.")[0];
    }

}