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

package de.welthungerhilfe.cgm.scanner.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class RecyclerMeasureAdapter extends RecyclerView.Adapter<RecyclerMeasureAdapter.ViewHolder> {
    private Context context;
    private List<Measure> measureList;
    private int lastPosition = -1;

    public RecyclerMeasureAdapter(Context ctx, List<Measure> ml) {
        context = ctx;
        measureList = ml;
    }

    @Override
    public int getItemViewType(int position) {
        Measure measure = measureList.get(position);
        if (measure.getType().equals(AppConstants.VAL_MEASURE_MANUAL))
            return 0;
        else
            return 1;
    }

    @Override
    public RecyclerMeasureAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = LayoutInflater.from(context).inflate(R.layout.row_measure_manual, parent, false);
            return new ViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.row_measure_machine, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerMeasureAdapter.ViewHolder holder, int position) {
        Measure measure = measureList.get(position);

        holder.editDate.setText(Utils.beautifyDate(measure.getDate()));
        if (measure.getLocation() == null)
            holder.editLocation.setText("Location not available");
        else
            holder.editLocation.setText(measure.getLocation().getAddress());
        holder.editHeight.setText(Float.toString(measure.getHeight()));
        holder.editWeight.setText(Float.toString(measure.getWeight()));
        holder.editMuac.setText(Float.toString(measure.getMuac()));

        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return measureList.size();
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    public void resetData(List<Measure> measureList) {
        this.measureList = measureList;
        notifyDataSetChanged();
    }

    public void addMeasure(Measure measure) {
        measureList.add(0, measure);
        notifyItemInserted(0);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public EditText editDate;
        public EditText editLocation;
        public EditText editHeight;
        public EditText editWeight;
        public EditText editMuac;

        public ViewHolder(View itemView) {
            super(itemView);

            editDate = itemView.findViewById(R.id.editDate);
            editLocation = itemView.findViewById(R.id.editLocation);
            editHeight = itemView.findViewById(R.id.editHeight);
            editWeight = itemView.findViewById(R.id.editWeight);
            editMuac = itemView.findViewById(R.id.editMuac);
        }
    }
}
