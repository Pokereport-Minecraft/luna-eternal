import os
import sys
import json
import csv

def audit_assets(resources_path):
    if not os.path.exists(resources_path):
        print(f"Error: La ruta {resources_path} no existe.")
        return

    # Estructuras esperadas
    models_path = os.path.join(resources_path, "assets", "cobblemon", "bedrock", "pokemon", "models")
    anims_path = os.path.join(resources_path, "assets", "cobblemon", "bedrock", "pokemon", "animations")
    tex_path = os.path.join(resources_path, "assets", "cobblemon", "textures", "pokemon")
    species_path = os.path.join(resources_path, "data", "cobblemon", "species")

    if not os.path.exists(species_path):
        print(f"No se encontró la carpeta 'species' en {species_path}. Asegúrate de apuntar a la carpeta 'resources'.")
        return

    missing_assets = []

    # Recorrer las especies para encontrar las de Gen 1 y 2
    for root, _, files in os.walk(species_path):
        for file in files:
            if file.endswith(".json"):
                filepath = os.path.join(root, file)
                try:
                    with open(filepath, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                    
                    dex = data.get("nationalPokedexNumber", 9999)
                    species_name = data.get("name", file.replace(".json", ""))
                    
                    # Filtrar gen 1 y 2 (1 a 251) o formas Mega/Gigamax
                    is_gen12 = 1 <= dex <= 251
                    
                    forms = data.get("forms", [])
                    has_mega_or_gmax = any("mega" in str(f.get("name", "")).lower() or "gmax" in str(f.get("name", "")).lower() or "gigantamax" in str(f.get("name", "")).lower() for f in forms)

                    if is_gen12 or has_mega_or_gmax:
                        form_names = [form.get("name") for form in forms]
                        if not form_names:
                            form_names = ["normal"]
                        
                        for form_name in form_names:
                            form_lower = form_name.lower()
                            search_name = species_name.lower()
                            
                            # Validar si nos interesan gen 1/2 o si es una mega/gmax especifica
                            if not is_gen12 and not ("mega" in form_lower or "gmax" in form_lower or "gigantamax" in form_lower):
                                continue

                            # En cobblemon los assets suelen tener el formato 0001_bulbasaur
                            dir_name = f"{dex:04d}_{search_name}"
                            
                            has_model = os.path.exists(os.path.join(models_path, dir_name)) or os.path.exists(os.path.join(models_path, search_name))
                            has_anim = os.path.exists(os.path.join(anims_path, dir_name)) or os.path.exists(os.path.join(anims_path, search_name))
                            has_tex = os.path.exists(os.path.join(tex_path, dir_name)) or os.path.exists(os.path.join(tex_path, search_name))
                            
                            # Validar forms específicos dentro de la carpeta
                            if not has_model and form_name != "normal":
                                dir_name_form = f"{dex:04d}_{search_name}_{form_lower}"
                                has_model = os.path.exists(os.path.join(models_path, dir_name_form)) or has_model
                                has_anim = os.path.exists(os.path.join(anims_path, dir_name_form)) or has_anim
                                has_tex = os.path.exists(os.path.join(tex_path, dir_name_form)) or has_tex

                            if not has_model or not has_anim or not has_tex:
                                missing_assets.append({
                                    "dex": dex,
                                    "species": species_name,
                                    "form": form_name,
                                    "missing_model": not has_model,
                                    "missing_anim": not has_anim,
                                    "missing_texture": not has_tex
                                })
                except Exception as e:
                    print(f"Error procesando {filepath}: {e}")

    # Generar reporte
    report_file = "pokemon_assets_audit.csv"
    with open(report_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=["dex", "species", "form", "missing_model", "missing_anim", "missing_texture"])
        writer.writeheader()
        writer.writerows(sorted(missing_assets, key=lambda x: x["dex"]))

    print(f"Auditoría completada. {len(missing_assets)} especies/formas de Gen 1 y 2 con assets faltantes.")
    print(f"Reporte generado en: {report_file}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python audit_pokemon_assets.py <ruta_a_assets_cobblemon>")
        print("Ejemplo: python audit_pokemon_assets.py vendor/cobblemon/common/src/main/resources/assets/cobblemon")
    else:
        audit_assets(sys.argv[1])
