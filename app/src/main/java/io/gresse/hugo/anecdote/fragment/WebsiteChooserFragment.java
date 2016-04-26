package io.gresse.hugo.anecdote.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.gresse.hugo.anecdote.R;
import io.gresse.hugo.anecdote.adapter.ViewHolderListener;
import io.gresse.hugo.anecdote.adapter.WebsiteChooserAdapter;
import io.gresse.hugo.anecdote.event.BusProvider;
import io.gresse.hugo.anecdote.event.ChangeTitleEvent;
import io.gresse.hugo.anecdote.event.LoadRemoteWebsiteEvent;
import io.gresse.hugo.anecdote.event.OnRemoteWebsiteResponseEvent;
import io.gresse.hugo.anecdote.event.WebsitesChangeEvent;
import io.gresse.hugo.anecdote.model.Website;
import io.gresse.hugo.anecdote.util.SpStorage;

/**
 * Display a list of website from Firebase so the user can choose which website he want to load after initial
 * application launch
 * <p/>
 * Created by Hugo Gresse on 03/03/16.
 */
public class WebsiteChooserFragment extends Fragment implements ViewHolderListener {

    @SuppressWarnings("unused")
    public static final String TAG = WebsiteChooserFragment.class.getSimpleName();

    public static final String BUNDLE_MODE_KEY     = "modeKey";
    public static final String BUNDLE_MODE_RESTORE = "restore";
    public static final String BUNDLE_MODE_ADD     = "add";

    @Bind(R.id.recyclerView)
    public RecyclerView mRecyclerView;

    @Bind(R.id.saveButton)
    public Button mSaveButton;

    @Nullable
    protected String                mMode;
    private   List<Website>         mWebsites;
    private   WebsiteChooserAdapter mAdapter;
    private   List<Website>         mSelectedWebsites;

    public static WebsiteChooserFragment newInstance(String mode) {
        WebsiteChooserFragment fragment = new WebsiteChooserFragment();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_MODE_KEY, mode);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_websitechooser, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            mMode = getArguments().getString(WebsiteChooserFragment.BUNDLE_MODE_KEY, null);
        }

        mSelectedWebsites = new ArrayList<>();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new WebsiteChooserAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        if (!TextUtils.isEmpty(mMode) && mMode.equals(BUNDLE_MODE_ADD)) {
            mSaveButton.setText(R.string.dialog_website_add);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
        BusProvider.getInstance().post(new ChangeTitleEvent(
                getString(R.string.dialog_websitechooser_title),
                this.getClass().getName()));


        if (mWebsites != null && !mWebsites.isEmpty()) {
            mAdapter.setData(mWebsites);
        } else {
            BusProvider.getInstance().post(new LoadRemoteWebsiteEvent());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @OnClick(R.id.saveButton)
    public void onSaveButtonClicked() {
        // If we are on the first launch, we assume the user didn't have any Websites, so we override it.
        // Else, we add the selected one
        if (TextUtils.isEmpty(mMode)) {
            SpStorage.saveWebsites(getActivity(), mSelectedWebsites);
            SpStorage.setFirstLaunch(getActivity(), false);
            BusProvider.getInstance().post(new WebsitesChangeEvent(true));
        } else if (mMode.equals(BUNDLE_MODE_RESTORE)) {
            SpStorage.saveWebsites(getActivity(), mSelectedWebsites);
            BusProvider.getInstance().post(new WebsitesChangeEvent(true));
        } else if (mMode.equals(BUNDLE_MODE_ADD)) {
            for (Website website : mSelectedWebsites) {
                SpStorage.saveWebsite(getActivity(), website);
            }
            BusProvider.getInstance().post(new WebsitesChangeEvent(false));
        }
    }


    /***************************
     * Event
     ***************************/

    @Subscribe
    public void onRemoteWebsiteLoaded(OnRemoteWebsiteResponseEvent event) {
        if (event.isSuccessful) {
            mWebsites = new ArrayList<>();
            mWebsites.addAll(event.websiteList);
            if (!TextUtils.isEmpty(mMode) && !mMode.equals(BUNDLE_MODE_RESTORE)) {
                // We want to add some websites : remove duplicates or already added ones
                List<Website> savedWebsites = SpStorage.getWebsites(getActivity());

                // We cannot iterate on a list and remove item at the same time, need an array
                for (Website website : mWebsites.toArray(new Website[0])) {
                    if (savedWebsites.contains(website)) {
                        mWebsites.remove(website);
                    }
                }
            }

            Collections.sort(mWebsites, new Comparator<Website>() {
                @Override
                public int compare(Website website, Website website2) {
                    if (website.like > website2.like) {
                        return -1;
                    } else if (website.like < website2.like) {
                        return 1;
                    }
                    return 0;
                }
            });

            mAdapter.setData(mWebsites);
        } else {
            Toast
                    .makeText(
                            getActivity(),
                            getActivity().getString(R.string.error_server_unknown),
                            Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /***************************
     * ViewHolderListener
     ***************************/

    @Override
    public void onClick(Object object) {
        if (object instanceof Integer) {
            FragmentManager fm = getActivity().getSupportFragmentManager();
            DialogFragment dialogFragment = WebsiteDialogFragment.newInstance(null);
            dialogFragment.show(fm, dialogFragment.getClass().getSimpleName());
        } else if (object instanceof Website) {

            Website website = (Website) object;
            if (mSelectedWebsites.contains(website)) {
                mSelectedWebsites.remove(website);
            } else {
                mSelectedWebsites.add(website);
            }
        }
    }

    @Override
    public void onLongClick(Object object) {

    }
}
