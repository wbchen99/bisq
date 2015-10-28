/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.gui.common.fxml;

import io.bitsquare.gui.common.ViewfxException;
import io.bitsquare.gui.common.view.FxmlView;
import io.bitsquare.gui.common.view.View;
import io.bitsquare.gui.common.view.ViewFactory;
import io.bitsquare.gui.common.view.ViewLoader;
import javafx.fxml.FXMLLoader;
import org.springframework.core.annotation.AnnotationUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;

public class FxmlViewLoader implements ViewLoader {

    private final ViewFactory viewFactory;
    private final ResourceBundle resourceBundle;

    @Inject
    public FxmlViewLoader(ViewFactory viewFactory, ResourceBundle resourceBundle) {
        this.viewFactory = viewFactory;
        this.resourceBundle = resourceBundle;
    }

    @SuppressWarnings("unchecked")
    public View load(Class<? extends View> viewClass) {
        FxmlView fxmlView = AnnotationUtils.getAnnotation(viewClass, FxmlView.class);

        final Class<? extends FxmlView.PathConvention> convention;
        final Class<? extends FxmlView.PathConvention> defaultConvention =
                (Class<? extends FxmlView.PathConvention>) getDefaultValue(FxmlView.class, "convention");

        final String specifiedLocation;
        final String defaultLocation = (String) getDefaultValue(FxmlView.class, "location");

        if (fxmlView == null) {
            convention = defaultConvention;
            specifiedLocation = defaultLocation;
        }
        else {
            convention = fxmlView.convention();
            specifiedLocation = fxmlView.location();
        }

        if (convention == null || specifiedLocation == null)
            throw new IllegalStateException("Convention and location should never be null.");


        try {
            final String resolvedLocation;
            if (specifiedLocation.equals(defaultLocation))
                resolvedLocation = convention.newInstance().apply(viewClass);
            else
                resolvedLocation = specifiedLocation;

            URL fxmlUrl = viewClass.getClassLoader().getResource(resolvedLocation);
            if (fxmlUrl == null)
                throw new ViewfxException(
                        "Failed to load view class [%s] because FXML file at [%s] could not be loaded " +
                                "as a classpath resource. Does it exist?", viewClass, specifiedLocation);

            return loadFromFxml(fxmlUrl);
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new ViewfxException(ex, "Failed to load view from class %s", viewClass);
        }
    }

    private View loadFromFxml(URL fxmlUrl) {
        checkNotNull(fxmlUrl, "FXML URL must not be null");
        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl, resourceBundle);
            loader.setControllerFactory(viewFactory);
            loader.load();
            Object controller = loader.getController();
            if (controller == null)
                throw new ViewfxException("Failed to load view from FXML file at [%s]. " +
                        "Does it declare an fx:controller attribute?", fxmlUrl);
            if (!(controller instanceof View))
                throw new ViewfxException("Controller of type [%s] loaded from FXML file at [%s] " +
                        "does not implement [%s] as expected.", controller.getClass(), fxmlUrl, View.class);
            return (View) controller;
        } catch (IOException ex) {
            throw new ViewfxException(ex, "Failed to load view from FXML file at [%s]", fxmlUrl);
        }
    }

}

