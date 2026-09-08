package com.mprlab.portal;

import android.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.widget.Spinner;

/** A selection dialog that participates in the Portal inactivity lifecycle. */
final class PortalSpinner extends Spinner {
    private final PortalActivity activity;

    PortalSpinner(PortalActivity activity) {
        super(new ContextThemeWrapper(activity, android.R.style.Theme_Material_Light), Spinner.MODE_DIALOG);
        this.activity = activity;
    }

    @Override public boolean performClick() {
        String[] labels = new String[getCount()];
        for (int index = 0; index < labels.length; index++) labels[index] = getItemAtPosition(index).toString();
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getPrompt())
                .setSingleChoiceItems(labels, getSelectedItemPosition(), (selected, index) -> {
                    setSelection(index);
                    selected.dismiss();
                }).create();
        activity.showPortalDialog(dialog);
        return true;
    }
}
