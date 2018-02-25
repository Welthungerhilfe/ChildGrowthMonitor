/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.welthungerhilfe.cgm.scanner.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.util.ArrayList;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.views.VerticalTextView;

public class GrowthDataFragment extends Fragment {
    private Context context;

    private ScatterChart chartGrowth;
    private MaterialSpinner dropChart;

    private VerticalTextView txtYAxis;
    private TextView txtXAxis;

    private TextView txtLabel;

    private int chartType = 1;

    public static GrowthDataFragment newInstance(Context context) {
        GrowthDataFragment fragment = new GrowthDataFragment();
        fragment.context = context;

        return fragment;
    }

    public void onResume() {
        super.onResume();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_growth, container, false);

        txtLabel = view.findViewById(R.id.txtLabel);
        if (((CreateDataActivity)context).person != null) {
            txtLabel.setText(((CreateDataActivity)context).person.getSex());
        }

        txtYAxis = view.findViewById(R.id.txtYAxis);
        txtXAxis = view.findViewById(R.id.txtXAxis);

        chartGrowth = view.findViewById(R.id.chartGrowth);
        dropChart = view.findViewById(R.id.dropChart);

        dropChart.setItems("Age / Height", "Age / Weight", "Height / Weight");
        dropChart.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                chartType = position + 1;
                setChartData();
            }
        });

        initChart();
        setChartData();

        return view;
    }

    public void setChartData() {
        if (chartType == 0 || context == null) {
            return;
        }
        if (((CreateDataActivity)context).measures == null || ((CreateDataActivity)context).measures.size() == 0) {
            return;
        }
        if (txtLabel == null) {
            return;
        }

        if (((CreateDataActivity)context).person != null) {
            txtLabel.setText(((CreateDataActivity)context).person.getSex());
        }

        ArrayList<Entry> yVals1 = new ArrayList<Entry>();
        for (int i = 0; i < ((CreateDataActivity)context).measures.size(); i++) {
            Measure measure = ((CreateDataActivity)context).measures.get(i);

            if (chartType == 1) {
                txtXAxis.setText("Age");
                txtYAxis.setText("Height");
                yVals1.add(new Entry(measure.getAge(), measure.getHeight()));
            } else if (chartType == 2) {
                txtXAxis.setText("Age");
                txtYAxis.setText("Weight");
                yVals1.add(new Entry(measure.getAge(), measure.getWeight()));
            } else if (chartType == 3) {
                txtXAxis.setText("Height");
                txtYAxis.setText("Weight");
                yVals1.add(new Entry(measure.getHeight(), measure.getWeight()));
            }
        }

        ScatterDataSet set1 = new ScatterDataSet(yVals1, "");
        set1.setScatterShapeHoleColor(getResources().getColor(R.color.colorPrimary));
        set1.setScatterShapeHoleRadius(5f);
        set1.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set1.setColor(getResources().getColor(R.color.colorWhite));
        set1.setScatterShapeSize(30f);

        ArrayList<IScatterDataSet> dataSets = new ArrayList<IScatterDataSet>();
        dataSets.add(set1);

        ScatterData data = new ScatterData(dataSets);
        chartGrowth.setData(data);
        chartGrowth.getXAxis().setAxisMinValue(-0.1f);
        chartGrowth.setVisibleXRangeMinimum(20);
        chartGrowth.invalidate();
    }

    private void initChart() {
        chartGrowth.getLegend().setEnabled(false);
        chartGrowth.getDescription().setEnabled(false);

        //chartGrowth.setBackgroundResource(R.drawable.back_chart);

        YAxis yAxis = chartGrowth.getAxisLeft();
        yAxis.setDrawGridLines(true);
        yAxis.enableGridDashedLine(5f, 5f, 0f);
        yAxis.setAxisMinimum(0f);
        yAxis.setGranularity(1f);

        chartGrowth.getAxisRight().setEnabled(false);

        XAxis xAxis = chartGrowth.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.enableGridDashedLine(5f, 5f, 0f);
        xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);

        chartGrowth.invalidate();
    }
}
