package com.example.listify.ui.lists;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import com.amplifyframework.auth.AuthException;
import com.example.listify.AuthManager;
import com.example.listify.CreateListAddDialogFragment;
import com.example.listify.CreateListDialogFragment;
import com.example.listify.ItemDetails;
import com.example.listify.ListPage;
import com.example.listify.LoadingCircleDialog;
import com.example.listify.R;
import com.example.listify.Requestor;
import com.example.listify.SearchResults;
import com.example.listify.SynchronousReceiver;
import com.example.listify.adapter.DisplayShoppingListsAdapter;
import com.example.listify.data.List;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Properties;

import static com.example.listify.MainActivity.am;

public class ListsFragment extends Fragment implements CreateListDialogFragment.OnNewListListener, Requestor.Receiver {
    ArrayList<List> shoppingLists = new ArrayList<>();
    DisplayShoppingListsAdapter displayShoppingListsAdapter;
    Requestor requestor;
    ListView shoppingListsView;
    ProgressBar loadingLists;
    int resultsIndex;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_lists, container, false);
        shoppingListsView = root.findViewById(R.id.shopping_lists);
        loadingLists = (ProgressBar) root.findViewById(R.id.progress_loading_lists);
        loadingLists.setVisibility(View.VISIBLE);

        Properties configs = new Properties();
        try {
            configs = AuthManager.loadProperties(getContext(), "android.resource://" + getActivity().getPackageName() + "/raw/auths.json");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        requestor = new Requestor(am, configs.getProperty("apiKey"));
        SynchronousReceiver<Integer[]> listIdsReceiver = new SynchronousReceiver<>();

        final Requestor.Receiver<Integer[]> recv = this;
        requestor.getListOfIds(List.class, recv, null);


        FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.new_list_fab);
        Fragment thisFragment = this;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateListDialogFragment createListDialogFragment = new CreateListDialogFragment();
                createListDialogFragment.show(getFragmentManager(), "Create New List");
                createListDialogFragment.setTargetFragment(thisFragment, 0);
            }
        });

        return root;
    }

    @Override
    public void sendNewListName(String name) {
        LoadingCircleDialog loadingDialog = new LoadingCircleDialog(getActivity());
        loadingDialog.show();

        Properties configs = new Properties();
        try {
            configs = AuthManager.loadProperties(getContext(), "android.resource://" + getActivity().getPackageName() + "/raw/auths.json");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        Requestor requestor = new Requestor(am, configs.getProperty("apiKey"));
        SynchronousReceiver<Integer> idReceiver = new SynchronousReceiver<>();

        List newList = new List(-1, name, "user filled by lambda", Instant.now().toEpochMilli());

        try {
            requestor.postObject(newList, idReceiver, idReceiver);
        } catch (Exception e) {
            Toast.makeText(getContext(), "An error occurred", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    newList.setItemID(idReceiver.await());
                } catch (Exception e) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "An error occurred", Toast.LENGTH_LONG).show();
                            loadingDialog.cancel();
                        }
                    });
                    e.printStackTrace();

                }
                shoppingLists.add(newList);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayShoppingListsAdapter.notifyDataSetChanged();
                        loadingDialog.cancel();
                        Toast.makeText(getContext(), String.format("%s created", name), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        t.start();
    }

    @Override
    public void acceptDelivery(Object delivered) {
        Integer[] listIds = (Integer[]) delivered;
        // Create threads and add them to a list
        Thread[] threads = new Thread[listIds.length];
        List[] results = new List[listIds.length];
        for (int i = 0; i < listIds.length; i++) {
            SynchronousReceiver<List> listReceiver = new SynchronousReceiver<>();
            requestor.getObject(Integer.toString(listIds[i]), List.class, listReceiver, listReceiver);
            final int finalI = i;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        results[finalI] = listReceiver.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            threads[i] = t;
            t.start();
        }

        // Wait for each thread to finish and add results to shoppingLists
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            shoppingLists.add(results[i]);
        }

        // Set adapter and display this users lists
        displayShoppingListsAdapter = new DisplayShoppingListsAdapter(getActivity(), shoppingLists);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                shoppingListsView.setAdapter(displayShoppingListsAdapter);
//                shoppingListsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                    @Override
//                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                        Intent listPage = new Intent(getContext(), ListPage.class);
//
//                        // Send the list ID
//                        listPage.putExtra("listID", shoppingLists.get(position).getItemID());
//                        startActivity(listPage);
//                    }
//                });
                loadingLists.setVisibility(View.GONE);
            }
        });

    }
}