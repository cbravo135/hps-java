package org.hps.online.recon.handlers;

import org.hps.online.recon.CommandHandler;
import org.hps.online.recon.CommandResult;
import org.hps.online.recon.CommandResult.JSONResult;
import org.hps.online.recon.Server;
import org.json.JSONObject;

/**
 * Handle the <i>status</i> command
 */
class StatusCommandHandler extends CommandHandler {

    StatusCommandHandler(Server server) {
        super(server);
    }

    public CommandResult execute(JSONObject jo) {

        boolean verbose = false;
        if (jo.has("verbose")) {
            verbose = jo.getBoolean("verbose");
        }

        JSONObject res = new JSONObject();

        // Put station status counts.
        JSONObject stationRes = new JSONObject();
        stationRes.put("total", mgr.getStationCount());
        stationRes.put("active", mgr.getActiveStations().size());
        stationRes.put("inactive", mgr.getInactiveStations().size());
        res.put("stations", stationRes);

        // Put ET system status.
        JSONObject etRes = new JSONObject();
        server.getEtStatus(etRes, verbose);
        res.put("ET", etRes);

        return new JSONResult(res);
    }

    @Override
    public String getCommandName() {
        return "status";
    }
}
