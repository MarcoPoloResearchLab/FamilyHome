package net.vantulder.tessel;

import android.content.Intent;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "PortalNavigation")
public class PortalNavigationPlugin extends Plugin {
    @PluginMethod public void home(PluginCall call) {
        getActivity().startActivity(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal.MainActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        call.resolve();
    }
}
