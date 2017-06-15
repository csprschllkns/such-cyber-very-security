/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controllers;

import Models.GeoIPv4;
import Models.GeoLocation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author melvin
 */
@Controller
@RequestMapping(value = "/index")
public class DashboardController {

    @RequestMapping()
    public ModelAndView home(Model model, HttpServletRequest request) {
        try {
            return alertFile();
        } catch (Exception e) {
            return new ModelAndView("index");
        }
    }

    private ModelAndView alertFile() {
        BufferedReader reader = null;
        List<JSONObject> JObjects = new ArrayList<>();
        try {
            File file = new File("/var/log/snort/alert.csv");
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");

                //In case of the alert message containing ','.
                String combinedMessage = "";
                for (int i = 1; i < data.length - 1; i++) {
                    combinedMessage += data[i];
                }

                String[] correctData = {
                    data[0],
                    combinedMessage,
                    data[2]
                };

                for (int i = 0; i < correctData.length; i++) {
                    correctData[i] = correctData[i].trim();
                }

                JSONObject json = new JSONObject();

                json.put("Hour", correctData[0].substring(6, correctData[0].indexOf(':')));
                json.put("Type", correctData[1]);

                //Only lookup a location if the source IP is not a local IP address.
                if (correctData[2].split(".")[0].equals("192")) {
                    json.put("Location", "Internal");
                } else {
                    GeoLocation location = GeoIPv4.getLocation(correctData[2]);

                    json.put("Longitude", location.getLongitude());
                    json.put("Latitude", location.getLatitude());
                    json.put("CountryName", location.getCountryName());
                }
                JObjects.add(json);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
            }
            return new ModelAndView("index", "JObjects", JObjects);
        }
    }
}