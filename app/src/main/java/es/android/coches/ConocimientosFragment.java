package es.android.coches;

import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import es.android.coches.databinding.FragmentConocimientosBinding;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ConocimientosFragment extends Fragment {

    private FragmentConocimientosBinding binding;

    List<PreguntaPais> todasLasPreguntas;
    List<String> todasLasRespuestas;
    int correcto = 0;

    List<PreguntaPais> preguntas;
    int respuestaCorrecta;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(todasLasPreguntas == null) {
            try {
                generarPreguntas("coches");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Collections.shuffle(todasLasPreguntas);
        preguntas = new ArrayList<>(todasLasPreguntas);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentConocimientosBinding.inflate(inflater,container,false);

        presentarPregunta();

        binding.botonRespuesta.setOnClickListener(v -> {
            int seleccionado = binding.radioGroup.getCheckedRadioButtonId();
            CharSequence mensaje = seleccionado == respuestaCorrecta ? "¡Acertaste!" : "Fallaste";
            if (mensaje.equals("¡Acertaste!")) {
                correcto++;
            }
            Snackbar.make(v, mensaje, Snackbar.LENGTH_INDEFINITE)
                    .setAction("Siguiente", v1 -> presentarPregunta())
                    .show();
            v.setEnabled(false);
        });

        return binding.getRoot();
    }

    private List<String> generarRespuestasPosibles(String respuestaCorrecta) {
        List<String> respuestasPosibles = new ArrayList<>();
        respuestasPosibles.add(respuestaCorrecta);

        List<String> respuestasIncorrectas = new ArrayList<>(todasLasRespuestas);
        respuestasIncorrectas.remove(respuestaCorrecta);

        for(int i=0; i<binding.radioGroup.getChildCount()-1; i++) {
            int indiceRespuesta = new Random().nextInt(respuestasIncorrectas.size());
            respuestasPosibles.add(respuestasIncorrectas.remove(indiceRespuesta));

        }
        Collections.shuffle(respuestasPosibles);
        return respuestasPosibles;
    }

    private void presentarPregunta() {
        if(preguntas.size() > 0) {
            binding.botonRespuesta.setEnabled(true);

            int pregunta = new Random().nextInt(preguntas.size());

            PreguntaPais preguntaActual = preguntas.remove(pregunta);
            preguntaActual.setRespuetas(generarRespuestasPosibles(preguntaActual.respuestaCorrecta));

            InputStream bandera = null;
            int idRecurso = getResources().getIdentifier(preguntaActual.bandera, "raw", getContext().getPackageName());
            bandera = getContext().getResources().openRawResource(idRecurso);
            binding.bandera.setImageBitmap(BitmapFactory.decodeStream(bandera));

            for (int i = 0; i < binding.radioGroup.getChildCount(); i++) {
                RadioButton radio = (RadioButton) binding.radioGroup.getChildAt(i);
                radio.setChecked(false);
                CharSequence respuesta = preguntaActual.getRespuetas().get(i);
                if (respuesta.equals(preguntaActual.respuestaCorrecta))
                    respuestaCorrecta = radio.getId();

                radio.setText(respuesta);
            }
        } else {
            binding.bandera.setVisibility(View.GONE);
            binding.radioGroup.setVisibility(View.GONE);
            binding.botonRespuesta.setVisibility(View.GONE);
            StringBuilder sb = new StringBuilder();
            try {
                FileInputStream fis = getContext().openFileInput("puntuacion.json");
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);

                String contenido = br.readLine();
                while (contenido != null) {
                    sb.append(contenido);
                    JSONObject objJSON = new JSONObject(sb.toString());
                    br.readLine();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            binding.textView.setText(sb.toString());
        }
    }


    class PreguntaPais {
        private String nombre;
        private String bandera;
        private String respuestaCorrecta;
        private List<String> respuetas;

        public PreguntaPais(String nombre, String foto) {
            this.nombre = nombre;
            this.bandera = foto;
            this.respuestaCorrecta = nombre;
        }

        public List<String> getRespuetas() {
            return respuetas;
        }

        public void setRespuetas(List<String> respuetas) {
            this.respuetas = respuetas;
        }
    }

    private Document leerXML(String fichero) throws Exception {
        int idRecurso = getResources().getIdentifier(fichero, "raw", getContext().getPackageName());
        InputStream isRecurso = getContext().getResources().openRawResource(idRecurso);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder constructor = factory.newDocumentBuilder();
        Document doc = constructor.parse(getContext().getResources().openRawResource(idRecurso));
        doc.getDocumentElement().normalize();
        return doc;
    }

    private void generarPreguntas(String fichero) throws Exception {
        todasLasPreguntas = new LinkedList<>();
        todasLasRespuestas = new LinkedList<>();
        Document doc = leerXML(fichero);
        Element documentElement = doc.getDocumentElement();
        NodeList paises = documentElement.getChildNodes();
        for(int i=0; i<paises.getLength(); i++) {
            if(paises.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element pais = (Element) paises.item(i);
                String nombre = pais.getElementsByTagName("nombre").item(0).getTextContent();
                String foto = pais.getElementsByTagName("foto").item(0).getTextContent();
                todasLasPreguntas.add(new PreguntaPais(nombre, foto));
                todasLasRespuestas.add(nombre);
            }
        }
    }
}