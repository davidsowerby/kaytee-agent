package uk.q3c.simplycd.agent;

import uk.q3c.krail.core.persist.clazz.i18n.EnumResourceBundle;

/**
 * Created by David Sowerby on 13 Mar 2017
 */
public class TestMessages extends EnumResourceBundle<TestMessageKey> {

    @Override
    protected void loadMap() {
        put(TestMessageKey.Wiggly, "Wiggly but cute");
    }
}
