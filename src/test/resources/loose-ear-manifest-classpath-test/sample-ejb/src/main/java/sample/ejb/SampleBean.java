package sample.ejb;

import sample.lib.Model;

public class SampleBean {

    public String value() {
        return new Model().name();
    }
}
