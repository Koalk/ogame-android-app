package com.wikaba.ogapp.agent.parsers;

import com.wikaba.ogapp.agent.models.BuildingInformation;

/**
 * Created by kevinleperf on 22/07/15.
 */
public class BuildingParser extends DefaultItemParser<BuildingInformation> {
    @Override
    protected BuildingInformation createDefaultItemInformationInstance() {
        return new BuildingInformation();
    }
}
