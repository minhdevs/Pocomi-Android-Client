package com.example.poly_truyen_client.ui.home;

import static org.greenrobot.eventbus.EventBus.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.poly_truyen_client.MainActivity;
import com.example.poly_truyen_client.R;
import com.example.poly_truyen_client.adapters.ComicsAdapter;
import com.example.poly_truyen_client.api.ComicServices;
import com.example.poly_truyen_client.api.ConnectAPI;
import com.example.poly_truyen_client.databinding.FragmentHomeBinding;
import com.example.poly_truyen_client.models.Comic;
import com.example.poly_truyen_client.models.User;
import com.example.poly_truyen_client.notifications.NotificationEvent;
import com.example.poly_truyen_client.socket.SocketConfig;
import com.example.poly_truyen_client.socket.SocketSingleton;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private SharedPreferences sharedPreferences;
    private ComicsAdapter comicsAdapter;
    private ComicServices comicServices;
    private User loggedInUser;
    private Socket socket;
    private ArrayList<Comic> list = new ArrayList<>();

    public HomeFragment() {
        this.socket = SocketSingleton.getInstance().getSocket();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        }
        getActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
        getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        socket.on("changeListComic", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        EventBus.getDefault().post(new NotificationEvent());
                        fetchComics();
                    }
                });
            }
        });

        sharedPreferences = getActivity().getSharedPreferences("poly_comic", Context.MODE_PRIVATE);

        comicServices = new ConnectAPI().connect.create(ComicServices.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        comicsAdapter = new ComicsAdapter(new ArrayList<Comic>(), getActivity());
        binding.rvComics.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        binding.rvComics.setAdapter(comicsAdapter);


        getLoggedInUser();
        fetchComics();

        // handle swipe refresh list comic
        binding.swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchComics();
                binding.swipeRefresh.setRefreshing(false);
            }
        });

        // handle search feature
        binding.edSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                ArrayList<Comic> listTmp = new ArrayList<>();

                String textSearch = s.toString().trim();

                if (textSearch.equals("")) {
                    comicsAdapter.updateList(list);

                } else {
                    for (Comic x: list) {
                        if (x.getName().toLowerCase().contains(textSearch.toLowerCase())) {
                            listTmp.add(x);
                        }
                    }

                    comicsAdapter.updateList(listTmp);

                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return root;
    }

    private void getLoggedInUser() {
        String userString = sharedPreferences.getString("user", "");
        loggedInUser = new Gson().fromJson(userString, User.class);

        binding.tvEmail.setText(loggedInUser.getUsername());
        binding.tvName.setText(loggedInUser.getFullName());

        if (loggedInUser.getAvatar() != null && !loggedInUser.getAvatar().equals("")) {
            Picasso.get().load(new ConnectAPI().API_URL + "images/" + loggedInUser.getAvatar()).into(binding.userAvatar);
        } else {
            Picasso.get().load(new ConnectAPI().API_URL + "images/" + "avatar-placeholder.png").into(binding.userAvatar);
        }
    }

    public void fetchComics() {

        ArrayList<Comic> listTmp = new ArrayList<>();

        Call<ArrayList<Comic>> getAllcomics = comicServices.getAllComics();
        getAllcomics.enqueue(new Callback<ArrayList<Comic>>() {
            @Override
            public void onResponse(Call<ArrayList<Comic>> call, Response<ArrayList<Comic>> response) {
                if (response.isSuccessful()) {
                    listTmp.addAll(response.body());
                    Collections.reverse(listTmp);
                    list.clear();
                    list.addAll(listTmp);
                    comicsAdapter.updateList(listTmp);
                }
            }

            @Override
            public void onFailure(Call<ArrayList<Comic>> call, Throwable throwable) {

            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNotificationReceived(NotificationEvent event) {
        fetchComics();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoggedInUser();
        fetchComics();
    }
}