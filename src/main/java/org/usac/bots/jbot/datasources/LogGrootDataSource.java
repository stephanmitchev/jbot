package org.usac.bots.jbot.datasources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class LogGrootDataSource {

    public static Logger log = null;

    private boolean enabled;

    protected LogGrootDataSource() {
        log = LoggerFactory.getLogger(this.getClass());
        enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
