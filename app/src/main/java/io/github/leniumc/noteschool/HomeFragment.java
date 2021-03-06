package io.github.leniumc.noteschool;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    private static final String SERVER_IP = "http://192.168.0.105/NoteSchool/";

    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayoutManager linearLayoutManager;
    private PostAdapter adapter;
    private FloatingActionButton floatingActionButton;

    private List<PostData> dataList;
    private int loadThreshold = 3;
    private int[] favPosts;

    // TODO: Rename and change types of parameters


    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = rootView.findViewById(R.id.recycler_view_home);
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        floatingActionButton = rootView.findViewById(R.id.add_button);

        if (dataList == null) {
            dataList = new ArrayList<>();
        }

        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new PostAdapter(getContext(), dataList);
        recyclerView.setAdapter(adapter);

        if (dataList.size() == 0) {
            loadData(dataList.size());
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                Log.d("complete", String.valueOf(linearLayoutManager.findLastCompletelyVisibleItemPosition()));
                Log.d("size", String.valueOf(dataList.size()));
                if (dy > 0 && floatingActionButton.getVisibility() == View.VISIBLE) {
                    floatingActionButton.hide();
                } else if (dy < 0 && floatingActionButton.getVisibility() != View.VISIBLE) {
                    floatingActionButton.show();
                }
                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() ==
                        dataList.size() - loadThreshold) {
                    loadData(dataList.size());
                }
            }
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), PublishActivity.class);
                getContext().startActivity(intent);
            }
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                clearRecyclerView();
            }
        });

        return rootView;
    }

    private void loadData(int id) {
        SharedPreferences preferences = getContext().getSharedPreferences("login_credentials", 0);
        String studentId = preferences.getString("user_id", null);
        String password = preferences.getString("password", null);
        new LoadDataTask().execute(studentId, password, String.valueOf(id));
    }

    private class LoadDataTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            final OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("user_id", params[0])
                    .add("user_password", params[1])
                    .add("row_num", params[2])
                    .build();
            Request request = new Request.Builder()
                    .url(SERVER_IP + "get_post.php")
                    .post(formBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                Log.d("tag", response.body().string());
                // TODO: complete this
            } catch (Exception e) {
                e.printStackTrace();
            }

            String[] str = {"lol", "123"};
            return str;
        }

        @Override
        protected void onPostExecute(String... list) {
            // TODO: get 10 posts
            for (String item: list) {
                Log.d("tag", item);
                // dataList.add(data);
            }
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.top_menu_search, menu);

        MaterialSearchView searchView = getActivity().findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Do some magic
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //Do some magic
                return false;
            }
        });

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                //Do some magic
            }

            @Override
            public void onSearchViewClosed() {
                //Do some magic
            }
        });

        final MenuItem item = menu.findItem(R.id.search);
        searchView.setMenuItem(item);
    }

    public void clearRecyclerView() {
        int size = dataList.size();
        dataList.clear();
        adapter.notifyItemRangeRemoved(0, size);
        loadData(0);
        swipeRefreshLayout.setRefreshing(false);
    }
}