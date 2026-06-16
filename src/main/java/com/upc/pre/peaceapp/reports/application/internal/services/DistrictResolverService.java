package com.upc.pre.peaceapp.reports.application.internal.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class DistrictResolverService {

    public static final String OUT_OF_COVERAGE = "Fuera de cobertura";

    private static final String GEOJSON_RESOURCE = "lima_callao_distritos.geojson";
    private final ObjectMapper objectMapper;
    private final List<DistrictFeature> districts = new ArrayList<>();
    private final Map<String, String> canonicalDistricts = new LinkedHashMap<>();

    public DistrictResolverService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        registerCanonicalDistricts();
    }

    @PostConstruct
    public void loadDistricts() {
        try (InputStream inputStream = new ClassPathResource(GEOJSON_RESOURCE).getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode features = root.path("features");

            for (JsonNode feature : features) {
                String rawName = firstText(
                        feature.path("properties").path("distrito2"),
                        feature.path("properties").path("distrito")
                );
                String districtName = canonicalizeDistrict(rawName);
                JsonNode geometry = feature.path("geometry");

                if (districtName == null || geometry.isMissingNode()) {
                    continue;
                }

                districts.add(new DistrictFeature(districtName, readGeometry(geometry)));
            }

            log.info("Loaded {} Lima/Callao district geometries", districts.size());
        } catch (Exception exception) {
            log.error("Could not load district GeoJSON: {}", exception.getMessage());
        }
    }

    public Optional<String> resolve(String latitude, String longitude) {
        try {
            double lat = Double.parseDouble(latitude);
            double lng = Double.parseDouble(longitude);
            return resolve(lat, lng);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public Optional<String> resolve(double latitude, double longitude) {
        return districts.stream()
                .filter(district -> district.contains(longitude, latitude))
                .map(DistrictFeature::name)
                .findFirst();
    }

    public String canonicalizeDistrict(String value) {
        String normalized = normalize(value);
        if (normalized == null) return null;

        if (normalized.equals("LIMA") || normalized.equals("CERCADO DE LIMA")) {
            return "Lima (Cercado)";
        }
        if (normalized.equals("CALLAO") || normalized.equals("CERCADO CALLAO") || normalized.equals("CERCADO DEL CALLAO")) {
            return "Callao (Cercado)";
        }
        if (normalized.startsWith("MI PER")) {
            return "Mi Peru";
        }

        return canonicalDistricts.getOrDefault(normalized, titleCase(normalized));
    }

    private void registerCanonicalDistricts() {
        List<String> names = List.of(
                "Ancon", "Ate", "Barranco", "Bellavista", "Brena", "Carabayllo",
                "Carmen de la Legua Reynoso", "Chaclacayo", "Chorrillos", "Cieneguilla",
                "Comas", "El Agustino", "Independencia", "Jesus Maria", "La Molina",
                "La Perla", "La Punta", "La Victoria", "Lima (Cercado)", "Lince",
                "Los Olivos", "Lurigancho-Chosica", "Lurin", "Magdalena del Mar",
                "Mi Peru", "Miraflores", "Pachacamac", "Pucusana", "Pueblo Libre",
                "Puente Piedra", "Punta Hermosa", "Punta Negra", "Rimac", "San Bartolo",
                "San Borja", "San Isidro", "San Juan de Lurigancho",
                "San Juan de Miraflores", "San Luis", "San Martin de Porres", "San Miguel",
                "Santa Anita", "Santa Maria del Mar", "Santa Rosa", "Santiago de Surco",
                "Surquillo", "Ventanilla", "Villa El Salvador", "Villa Maria del Triunfo"
        );

        names.forEach(name -> canonicalDistricts.put(normalize(name), name));
        canonicalDistricts.put("LURIGANCHO", "Lurigancho-Chosica");
    }

    private List<Polygon> readGeometry(JsonNode geometry) {
        String type = geometry.path("type").asText();
        JsonNode coordinates = geometry.path("coordinates");
        List<Polygon> polygons = new ArrayList<>();

        if ("Polygon".equalsIgnoreCase(type)) {
            polygons.add(readPolygon(coordinates));
        } else if ("MultiPolygon".equalsIgnoreCase(type)) {
            for (JsonNode polygonNode : coordinates) {
                polygons.add(readPolygon(polygonNode));
            }
        }

        return polygons;
    }

    private Polygon readPolygon(JsonNode polygonNode) {
        List<List<Point>> rings = new ArrayList<>();
        for (JsonNode ringNode : polygonNode) {
            List<Point> ring = new ArrayList<>();
            for (JsonNode pointNode : ringNode) {
                ring.add(new Point(pointNode.get(0).asDouble(), pointNode.get(1).asDouble()));
            }
            rings.add(ring);
        }
        return new Polygon(rings);
    }

    private String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull() && !node.asText().isBlank()) {
                return node.asText();
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace('Ñ', 'N')
                .replaceAll("[^A-Z0-9() ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String titleCase(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).split(" ");
        List<String> titled = new ArrayList<>();
        for (String word : words) {
            if (word.isBlank()) continue;
            titled.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
        }
        return String.join(" ", titled);
    }

    private record DistrictFeature(String name, List<Polygon> polygons) {
        boolean contains(double lng, double lat) {
            return polygons.stream().anyMatch(polygon -> polygon.contains(lng, lat));
        }
    }

    private record Polygon(List<List<Point>> rings) {
        boolean contains(double lng, double lat) {
            if (rings.isEmpty() || !insideRing(rings.get(0), lng, lat)) return false;
            for (int i = 1; i < rings.size(); i++) {
                if (insideRing(rings.get(i), lng, lat)) return false;
            }
            return true;
        }

        private boolean insideRing(List<Point> ring, double lng, double lat) {
            boolean inside = false;
            for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
                Point pi = ring.get(i);
                Point pj = ring.get(j);
                boolean intersects = ((pi.lat() > lat) != (pj.lat() > lat))
                        && (lng < (pj.lng() - pi.lng()) * (lat - pi.lat()) / (pj.lat() - pi.lat()) + pi.lng());
                if (intersects) inside = !inside;
            }
            return inside;
        }
    }

    private record Point(double lng, double lat) {
    }
}
