package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.connector.UserAction.Context;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public abstract class UserClickListener implements View.OnClickListener {

    @NonNull private final Context user;

    private UserClickListener(@NonNull final UserAction.Context user) {
        this.user = user;
    }

    @Override
    public void onClick(final View view) {
        if (view == null) {
            return;
        }

        showUserActionsDialog(view);
    }

    @NonNull
    protected abstract List<UserAction> createUserActions(UserAction.Context user);

    /**
     * Opens a dialog to do actions on a user name
     */
    private void showUserActionsDialog(final View view) {
        final AbstractActivity activity = (AbstractActivity) view.getContext();
        user.setActivity(activity);

        final List<UserAction> userActions = createUserActions(user);
        if (userActions.isEmpty()) {
            return;
        }

        final Resources res = activity.getResources();

        final ArrayList<String> labels = new ArrayList<>(userActions.size());
        for (final UserAction action : userActions) {
            labels.add(res.getString(action.displayResourceId));
        }
        final CharSequence[] items = labels.toArray(new String[labels.size()]);

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(res.getString(R.string.user_menu_title) + " " + user.userName);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int item) {
                userActions.get(item).run(user);
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public static UserClickListener forOwnerOf(final Geocache cache) {
        return forUser(cache, cache.getOwnerDisplayName(), cache.getOwnerUserId());
    }

    public static OnClickListener forOwnerOf(final Trackable trackable) {
        return forUser(trackable, trackable.getOwner());
    }

    public static OnClickListener forUser(final Trackable trackable, final String userName) {
        return new UserClickListener(new Context(userName, StringUtils.EMPTY)) {

            @Override
            protected List<UserAction> createUserActions(final UserAction.Context user) {
                return ConnectorFactory.getConnector(trackable).getUserActions(user);
            }
        };
    }

    public static UserClickListener forUser(final Geocache cache, final String userName, final String userId) {
        return new UserClickListener(new Context(userName, userId)) {

            @Override
            protected List<UserAction> createUserActions(final UserAction.Context user) {
                return ConnectorFactory.getConnector(cache).getUserActions(user);
            }
        };
    }

}
