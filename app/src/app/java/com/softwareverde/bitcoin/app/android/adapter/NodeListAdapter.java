package com.softwareverde.bitcoin.app.android.adapter;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.softwareverde.bitcoin.app.R;
import com.softwareverde.bitcoin.android.lib.Node;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.util.StringUtil;

import java.util.Comparator;

public class NodeListAdapter extends RecyclerView.Adapter<NodeListAdapter.ViewHolder> {
    protected Activity _activity;
    protected LayoutInflater _inflater;
    protected MutableList<Node> _dataSet = new MutableList<Node>();

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final View view;
        public final TextView userAgentView;
        public final TextView pingView;
        public final TextView ipView;

        public ViewHolder(@NonNull final View view) {
            super(view);

            this.view = view;
            this.userAgentView = view.findViewById(R.id.peer_item_user_agent);
            this.pingView = view.findViewById(R.id.peer_item_ping);
            this.ipView = view.findViewById(R.id.peer_item_ip);
        }
    }

    private void _sortDataSet() {
        _dataSet.sort(new Comparator<Node>() {
            @Override
            public int compare(final Node node0, final Node node1) {
                int pingCompare = (node0.getPing().compareTo(node1.getPing()));
                if (pingCompare != 0) { return pingCompare; }

                return ( (node0.getIp()).compareTo(node1.getIp()) );
            }
        });
    }

    public NodeListAdapter(final Activity activity) {
        _activity = activity;
        _inflater = LayoutInflater.from(_activity.getApplicationContext());
    }

    @Override
    public int getItemCount() {
        return _dataSet.getSize();
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int position) {
        final Node node = _dataSet.get(position);

        viewHolder.userAgentView.setText(node.getUserAgent());
        viewHolder.pingView.setText(StringUtil.formatNumberString(node.getPing().intValue()) + "ms");
        viewHolder.ipView.setText(node.getIp() + ":" + node.getPort());
    }

    @NonNull
    @Override
    public NodeListAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup viewGroup, final int position) {
        final View view = _inflater.inflate(R.layout.node_item, viewGroup, false);
        return new ViewHolder(view);
    }


    public Node getItem(final int position) {
        if ( (position < 0) || (position >= _dataSet.getSize()) ) { return null; }

        return _dataSet.get(position);
    }

    public boolean isEmpty() {
        return (_dataSet.getSize() == 0);
    }

    public void add(final Node node) {
        _dataSet.add(node);

        _sortDataSet();

        this.notifyDataSetChanged();
    }

    public void addAll(final List<Node> peers) {
        _dataSet.addAll(peers);

        _sortDataSet();

        this.notifyDataSetChanged();
    }

    public void clear() {
        _dataSet.clear();

        this.notifyDataSetChanged();
    }
}
