package com.mgilangjanuar.dev.goscele.modules.main.view;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import com.mgilangjanuar.dev.goscele.R;
import com.mgilangjanuar.dev.goscele.base.BaseActivity;
import com.mgilangjanuar.dev.goscele.base.BaseFragment;
import com.mgilangjanuar.dev.goscele.modules.main.adapter.ScheduleDeadlineRecyclerViewAdapter;
import com.mgilangjanuar.dev.goscele.modules.main.listener.ScheduleDeadlineDetailListener;
import com.mgilangjanuar.dev.goscele.modules.main.listener.ScheduleDeadlineListener;
import com.mgilangjanuar.dev.goscele.modules.main.model.ScheduleDeadlineDaysModel;
import com.mgilangjanuar.dev.goscele.modules.main.presenter.ScheduleDeadlinePresenter;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;

public class ScheduleDeadlineFragment extends BaseFragment implements ScheduleDeadlineListener, ScheduleDeadlineDetailListener {

    @BindView(R.id.calendar_view)
    MaterialCalendarView materialCalendarView;

    @BindView(R.id.swipe_refresh_schedule)
    SwipeRefreshLayout swipeRefreshLayout;

    TextView titleSlidingUpPanel;
    RecyclerView recyclerView;
    SlidingUpPanelLayout slidingUpPanelLayout;
    TextView status;

    private ScheduleDeadlinePresenter presenter = new ScheduleDeadlinePresenter(this, this);
    private Date date;

    public static ScheduleDeadlineFragment newInstance(TextView titleSlidingUpPanel, RecyclerView recyclerView, SlidingUpPanelLayout slidingUpPanelLayout, TextView status) {
        ScheduleDeadlineFragment fragment = new ScheduleDeadlineFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        fragment.titleSlidingUpPanel = titleSlidingUpPanel;
        fragment.recyclerView = recyclerView;
        fragment.slidingUpPanelLayout = slidingUpPanelLayout;
        fragment.status = status;
        return fragment;
    }

    @Override
    public int findLayout() {
        return R.layout.fragment_schedule_deadline;
    }

    @Override
    public void initialize(Bundle savedInstanceState) {
        super.initialize(savedInstanceState);
        date = new Date();

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        presenter.getDeadlineDays(date.getTime() / 1000);

        materialCalendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                ScheduleDeadlineFragment.this.date = date.getDate();
                updateDeadlineDays();
            }
        });

        materialCalendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull final CalendarDay date, boolean selected) {
                ScheduleDeadlineFragment.this.date = date.getDate();
                materialCalendarView.clearSelection();
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                updateDetails();
            }
        });

        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                titleSlidingUpPanel.setText(new SimpleDateFormat(slideOffset > 0.5 ? "MMMM dd, yyyy" : "MMMM yyyy").format(date.getTime()));
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState.equals(SlidingUpPanelLayout.PanelState.EXPANDED)
                        && !presenter.validateAdapterCurrentDate(recyclerView.getAdapter(), date)) {
                    updateDetails();
                }
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                presenter.clearDeadlineDays(date);
                updateDeadlineDays();
                updateDetails();
            }
        });
    }

    @Override
    public void onRetrieveDeadlineDays(final ScheduleDeadlineDaysModel model) {
        swipeRefreshLayout.setRefreshing(false);
        if (slidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
            titleSlidingUpPanel.setText(new SimpleDateFormat("MMMM yyyy").format(date.getTime()));
        }
        materialCalendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.getMonth() == model.month && model.getDays().contains(day.getDay());
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(), android.R.color.white)));
                view.setSelectionDrawable(ContextCompat.getDrawable(getContext(), R.drawable.round));
            }
        });
    }

    @Override
    public void onRetrieveDeadlineDetail(ScheduleDeadlineRecyclerViewAdapter adapter) {
        if (adapter.getItemCount() == 0) {
            status.setText(getString(R.string.empty));
            status.setTextColor(getResources().getColor(R.color.color_accent));
        }
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onError(String error) {
        if (getActivity() != null) ((BaseActivity) getActivity()).showSnackbar(error);
    }

    private void updateDeadlineDays() {
        titleSlidingUpPanel.setText(getString(R.string.loading));
        materialCalendarView.removeDecorators();
        presenter.getDeadlineDays(ScheduleDeadlineFragment.this.date.getTime() / 1000);
    }

    private void updateDetails() {
        status.setText(getString(R.string.loading));
        status.setTextColor(getResources().getColor(android.R.color.darker_gray));
        recyclerView.setAdapter(presenter.buildEmptyAdapter(date));
        presenter.getDeadlineDetail(ScheduleDeadlineFragment.this.date.getTime() / 1000);
    }
}
