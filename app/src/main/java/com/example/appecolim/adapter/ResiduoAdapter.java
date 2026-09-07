package com.example.appecolim.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appecolim.R;
import com.example.appecolim.data.model.Residuo;
import com.example.appecolim.utils.FechaUtils;
import java.util.List;
import java.util.Locale;

public class ResiduoAdapter extends RecyclerView.Adapter<ResiduoAdapter.ResiduoViewHolder> {

    private List<Residuo> listaResiduos;
    private boolean esHistorial;
    public interface OnEditarListener {
        void onEditar(Residuo residuo);
    }

    private OnEditarListener editarListener;

    public void setOnEditarListener(OnEditarListener listener) {
        this.editarListener = listener;
    }

    public ResiduoAdapter(List<Residuo> listaResiduos, boolean esHistorial) {
        this.listaResiduos = listaResiduos;
        this.esHistorial = esHistorial;
    }

    @NonNull
    @Override
    public ResiduoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Seleccionamos el layout correcto según si es historial o registro diario
        int layoutId = esHistorial ? R.layout.item_residuo_historial : R.layout.item_residuo;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ResiduoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResiduoViewHolder holder, int position) {
        Residuo residuo = listaResiduos.get(position);

        // --- LÓGICA DE FECHA/HORA AUTOMÁTICA ---
        if (esHistorial) {
            // En historial mostramos la fecha abreviada (ej: 06/09)
            holder.txtFechaHora.setText(FechaUtils.extraerFecha(residuo.getFechaHora()));
        } else {
            // En registro diario mostramos solo la hora (ej: 14:30)
            holder.txtFechaHora.setText(FechaUtils.extraerHora(residuo.getFechaHora()));
        }

        holder.txtTipo.setText(residuo.getTipo());
        holder.txtCantidadKg.setText( String.format(Locale.getDefault(), "%.2f Kg", residuo.getCantidadKg()) );

        // Manejo del botón editar (solo presente en item_residuo)
        if (holder.btnEditar != null) {
            holder.btnEditar.setVisibility(esHistorial ? View.GONE : View.VISIBLE);

            holder.btnEditar.setOnClickListener(v -> {
                int posicion = holder.getBindingAdapterPosition();

                if (!esHistorial
                        && editarListener != null
                        && posicion != RecyclerView.NO_POSITION) {
                    editarListener.onEditar(listaResiduos.get(posicion));
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return listaResiduos.size();
    }

    public static class ResiduoViewHolder extends RecyclerView.ViewHolder {
        TextView txtFechaHora, txtTipo, txtCantidadKg;
        View btnEditar;

        public ResiduoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtFechaHora = itemView.findViewById(R.id.txt_fecha_hora);
            txtTipo = itemView.findViewById(R.id.txt_tipo);
            txtCantidadKg = itemView.findViewById(R.id.txt_cantidad_kg);
            btnEditar = itemView.findViewById(R.id.btn_editar);
        }
    }
}
