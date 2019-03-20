package com.github.anrimian.musicplayer.ui.common.order;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.github.anrimian.musicplayer.R;
import com.github.anrimian.musicplayer.di.Components;
import com.github.anrimian.musicplayer.domain.models.composition.order.Order;
import com.github.anrimian.musicplayer.domain.models.composition.order.OrderType;
import com.github.anrimian.musicplayer.ui.common.order.adapter.OrderAdapter;
import com.github.anrimian.musicplayer.ui.utils.OnCompleteListener;
import com.github.anrimian.musicplayer.ui.utils.moxy.ui.MvpAppCompatDialogFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

import static com.github.anrimian.musicplayer.Constants.Arguments.ORDER_ARG;
import static com.github.anrimian.musicplayer.ui.common.format.FormatUtils.getReversedOrderText;

public class SelectOrderDialogFragment extends MvpAppCompatDialogFragment implements SelectOrderView {

    @InjectPresenter
    SelectOrderPresenter presenter;

    @BindView(R.id.rv_order)
    RecyclerView rvOrder;

    @BindView(R.id.cb_desc)
    CheckBox cbDesc;

    private OrderAdapter orderAdapter;

    @ProvidePresenter
    SelectOrderPresenter providePresenter() {
        return Components.getLibraryComponent().selectOrderPresenter();
    }

    @Nullable
    private OnCompleteListener<Order> onCompleteListener;

    public static SelectOrderDialogFragment newInstance(Order order) {
        Bundle args = new Bundle();
        args.putSerializable(ORDER_ARG, order);
        SelectOrderDialogFragment fragment = new SelectOrderDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.dialog_order, null);

        ButterKnife.bind(this, view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        rvOrder.setLayoutManager(layoutManager);

        orderAdapter = new OrderAdapter();
        orderAdapter.setOnItemClickListener(presenter::onOrderTypeSelected);
        rvOrder.setAdapter(orderAdapter);

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.order)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.cancel, (dialog1, which) -> {})
                .create();
        dialog.show();

        Button btnOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnOk.setOnClickListener(v -> presenter.onCompleteButtonClicked());

        cbDesc.setOnCheckedChangeListener((buttonView, isChecked) ->
                presenter.onReverseTypeSelected(isChecked)
        );

        if (savedInstanceState == null) {
            presenter.setOrder(getOrder());
        }

        return dialog;

    }

    @Override
    public void showSelectedOrder(OrderType orderType) {
        orderAdapter.setCheckedItem(orderType);
        cbDesc.setText(getReversedOrderText(orderType));
    }

    @Override
    public void showReverse(boolean selected) {
        cbDesc.setChecked(selected);
    }

    @Override
    public void close(Order order) {
        if (!getOrder().equals(order) && onCompleteListener != null) {
            onCompleteListener.onComplete(order);
        }
        dismiss();
    }

    public void setOnCompleteListener(@Nullable OnCompleteListener<Order> onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    private Order getOrder() {
        //noinspection ConstantConditions
        return (Order) getArguments().getSerializable(ORDER_ARG);
    }
}
