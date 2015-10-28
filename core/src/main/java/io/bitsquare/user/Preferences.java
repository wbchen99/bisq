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

package io.bitsquare.user;

import io.bitsquare.app.BitsquareEnvironment;
import io.bitsquare.app.Version;
import io.bitsquare.btc.BitcoinNetwork;
import io.bitsquare.locale.CurrencyUtil;
import io.bitsquare.locale.TradeCurrency;
import io.bitsquare.storage.Storage;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.bitcoinj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;

public class Preferences implements Serializable {
    // That object is saved to disc. We need to take care of changes to not break deserialization.
    private static final long serialVersionUID = Version.LOCAL_DB_VERSION;

    transient private static final Logger log = LoggerFactory.getLogger(Preferences.class);

    // Deactivate mBit for now as most screens are not supporting it yet
    transient private static final List<String> BTC_DENOMINATIONS = Arrays.asList(MonetaryFormat.CODE_BTC/*, MonetaryFormat.CODE_MBTC*/);
    transient static final private ArrayList<BlockChainExplorer> blockChainExplorersTestNet = new ArrayList<>(Arrays.asList(
            new BlockChainExplorer("Blocktrail", "https://www.blocktrail.com/tBTC/tx/", "https://www.blocktrail.com/tBTC/address/"),
            new BlockChainExplorer("Blockr.io", "https://tbtc.blockr.io/tx/info/", "https://tbtc.blockr.io/address/info/"),
            new BlockChainExplorer("Web BTC", "http://test.webbtc.com/tx/", "http://test.webbtc.com/address/"),
            new BlockChainExplorer("Blockexplorer", "https://blockexplorer.com/testnet/tx/", "https://blockexplorer.com/testnet/address/"),
            new BlockChainExplorer("Biteasy", "https://www.biteasy.com/testnet/transactions/", "https://www.biteasy.com/testnet/addresses/")
    ));

    transient static final private ArrayList<BlockChainExplorer> blockChainExplorersMainNet = new ArrayList<>(Arrays.asList(
            new BlockChainExplorer("Blockchain.info", "https://blockchain.info/tx/", "https://blockchain.info/address/"),
            new BlockChainExplorer("Blocktrail", "https://www.blocktrail.com/BTC/tx/", "https://www.blocktrail.com/BTC/address/"),
            new BlockChainExplorer("Blockr.io", "https://btc.blockr.io/tx/info/", "https://btc.blockr.io/address/info/"),
            new BlockChainExplorer("Web BTC", "http://webbtc.com/tx/", "http://webbtc.com/address/"),
            new BlockChainExplorer("Blockexplorer", "https://blockexplorer.com/tx/", "https://blockexplorer.com/address/"),
            new BlockChainExplorer("Biteasy", "https://www.biteasy.com/transactions/", "https://www.biteasy.com/addresses/")
    ));

    public static List<String> getBtcDenominations() {
        return BTC_DENOMINATIONS;
    }

    public static Locale getDefaultLocale() {
        //return new Locale("EN", "US");
        return Locale.getDefault();
    }

    transient private final Storage<Preferences> storage;
    transient private final BitsquareEnvironment bitsquareEnvironment;

    transient private BitcoinNetwork bitcoinNetwork;

    // Persisted fields
    private String btcDenomination = MonetaryFormat.CODE_BTC;
    private boolean useAnimations = true;
    private boolean useEffects = true;
    private boolean displaySecurityDepositInfo = true;
    private boolean useUPnP = true;
    private ArrayList<TradeCurrency> tradeCurrencies;
    private BlockChainExplorer blockChainExplorerMainNet;
    private BlockChainExplorer blockChainExplorerTestNet;
    private boolean showPlaceOfferConfirmation;
    private boolean showTakeOfferConfirmation;
    private String backupDirectory;
    private boolean autoSelectArbitrators = true;
    private Map<String, Boolean> showAgainMap;
    private boolean tacAccepted;

    // Observable wrappers
    transient private final StringProperty btcDenominationProperty = new SimpleStringProperty(btcDenomination);
    transient private final BooleanProperty useAnimationsProperty = new SimpleBooleanProperty(useAnimations);
    transient private final BooleanProperty useEffectsProperty = new SimpleBooleanProperty(useEffects);
    transient private final ObservableList<TradeCurrency> tradeCurrenciesAsObservable = FXCollections.observableArrayList();


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public Preferences(Storage<Preferences> storage, BitsquareEnvironment bitsquareEnvironment) {
        log.debug("Preferences " + this);
        this.storage = storage;
        this.bitsquareEnvironment = bitsquareEnvironment;

        Preferences persisted = storage.initAndGetPersisted(this);
        if (persisted != null) {
            setBtcDenomination(persisted.btcDenomination);
            setUseAnimations(persisted.useAnimations);
            setUseEffects(persisted.useEffects);
            setUseUPnP(persisted.useUPnP);
            setTradeCurrencies(persisted.tradeCurrencies);
            tradeCurrencies = new ArrayList<>(tradeCurrenciesAsObservable);
            displaySecurityDepositInfo = persisted.getDisplaySecurityDepositInfo();

            setBlockChainExplorerTestNet(persisted.getBlockChainExplorerTestNet());
            setBlockChainExplorerMainNet(persisted.getBlockChainExplorerMainNet());
            // In case of an older version without that data we set it to defaults
            if (blockChainExplorerTestNet == null)
                setBlockChainExplorerTestNet(blockChainExplorersTestNet.get(0));
            if (blockChainExplorerMainNet == null)
                setBlockChainExplorerTestNet(blockChainExplorersMainNet.get(0));

            showPlaceOfferConfirmation = persisted.getShowPlaceOfferConfirmation();
            showTakeOfferConfirmation = persisted.getShowTakeOfferConfirmation();
            backupDirectory = persisted.getBackupDirectory();
            autoSelectArbitrators = persisted.getAutoSelectArbitrators();
            showAgainMap = persisted.getShowAgainMap();
            tacAccepted = persisted.getTacAccepted();
        } else {
            setTradeCurrencies(CurrencyUtil.getAllSortedCurrencies());
            tradeCurrencies = new ArrayList<>(tradeCurrenciesAsObservable);
            setBlockChainExplorerTestNet(blockChainExplorersTestNet.get(0));
            setBlockChainExplorerMainNet(blockChainExplorersMainNet.get(0));
            showPlaceOfferConfirmation = true;
            showTakeOfferConfirmation = true;

            showAgainMap = new HashMap<>();
            showAgainMap.put(PopupId.SEC_DEPOSIT, true);
            showAgainMap.put(PopupId.TRADE_WALLET, true);

            storage.queueUpForSave();
        }


        this.bitcoinNetwork = bitsquareEnvironment.getBitcoinNetwork();

        // Use that to guarantee update of the serializable field and to make a storage update in case of a change
        btcDenominationProperty.addListener((ov) -> {
            btcDenomination = btcDenominationProperty.get();
            storage.queueUpForSave();
        });
        useAnimationsProperty.addListener((ov) -> {
            useAnimations = useAnimationsProperty.get();
            storage.queueUpForSave();
        });
        useEffectsProperty.addListener((ov) -> {
            useEffects = useEffectsProperty.get();
            storage.queueUpForSave();
        });
        tradeCurrenciesAsObservable.addListener((Observable ov) -> {
            tradeCurrencies.clear();
            tradeCurrencies.addAll(tradeCurrenciesAsObservable);
            storage.queueUpForSave();
        });
    }

    public void dontShowAgain(String id) {
        showAgainMap.put(id, false);
        storage.queueUpForSave();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Setter
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void setBtcDenomination(String btcDenominationProperty) {
        this.btcDenominationProperty.set(btcDenominationProperty);
    }

    public void setUseAnimations(boolean useAnimationsProperty) {
        this.useAnimationsProperty.set(useAnimationsProperty);
    }

    public void setUseEffects(boolean useEffectsProperty) {
        this.useEffectsProperty.set(useEffectsProperty);
    }

    public void setDisplaySecurityDepositInfo(boolean displaySecurityDepositInfo) {
        this.displaySecurityDepositInfo = displaySecurityDepositInfo;
        storage.queueUpForSave();
    }

    public void setUseUPnP(boolean useUPnP) {
        this.useUPnP = useUPnP;
        storage.queueUpForSave();
    }

    public void setBitcoinNetwork(BitcoinNetwork bitcoinNetwork) {
        if (this.bitcoinNetwork != bitcoinNetwork)
            bitsquareEnvironment.saveBitcoinNetwork(bitcoinNetwork);

        this.bitcoinNetwork = bitcoinNetwork;
        storage.queueUpForSave();
    }

    private void setTradeCurrencies(List<TradeCurrency> tradeCurrencies) {
        tradeCurrenciesAsObservable.setAll(tradeCurrencies);
    }

    private void setBlockChainExplorerTestNet(BlockChainExplorer blockChainExplorerTestNet) {
        this.blockChainExplorerTestNet = blockChainExplorerTestNet;
        storage.queueUpForSave();
    }

    private void setBlockChainExplorerMainNet(BlockChainExplorer blockChainExplorerMainNet) {
        this.blockChainExplorerMainNet = blockChainExplorerMainNet;
        storage.queueUpForSave();
    }

    public void setBlockChainExplorer(BlockChainExplorer blockChainExplorer) {
        if (bitcoinNetwork == BitcoinNetwork.MAINNET)
            setBlockChainExplorerMainNet(blockChainExplorer);
        else
            setBlockChainExplorerTestNet(blockChainExplorer);
    }

    public void setShowPlaceOfferConfirmation(boolean showPlaceOfferConfirmation) {
        this.showPlaceOfferConfirmation = showPlaceOfferConfirmation;
        storage.queueUpForSave();
    }

    public void setShowTakeOfferConfirmation(boolean showTakeOfferConfirmation) {
        this.showTakeOfferConfirmation = showTakeOfferConfirmation;
        storage.queueUpForSave();
    }

    public void setTacAccepted(boolean tacAccepted) {
        this.tacAccepted = tacAccepted;
        storage.queueUpForSave();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getter
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String getBtcDenomination() {
        return btcDenominationProperty.get();
    }

    public boolean getUseEffects() {
        return useEffectsProperty.get();
    }

    public boolean getUseAnimations() {
        return useAnimationsProperty.get();
    }

    public boolean getDisplaySecurityDepositInfo() {
        return displaySecurityDepositInfo;
    }

    public StringProperty btcDenominationProperty() {
        return btcDenominationProperty;
    }

    public BooleanProperty useAnimationsProperty() {
        return useAnimationsProperty;
    }

    public BooleanProperty useEffectsPropertyProperty() {
        return useEffectsProperty;
    }

    public boolean getUseUPnP() {
        return useUPnP;
    }

    public BitcoinNetwork getBitcoinNetwork() {
        return bitcoinNetwork;
    }

    public ObservableList<TradeCurrency> getTradeCurrenciesAsObservable() {
        return tradeCurrenciesAsObservable;
    }

    private BlockChainExplorer getBlockChainExplorerTestNet() {
        return blockChainExplorerTestNet;
    }

    private BlockChainExplorer getBlockChainExplorerMainNet() {
        return blockChainExplorerMainNet;
    }

    public BlockChainExplorer getBlockChainExplorer() {
        if (bitcoinNetwork == BitcoinNetwork.MAINNET)
            return blockChainExplorerMainNet;
        else
            return blockChainExplorerTestNet;
    }

    public ArrayList<BlockChainExplorer> getBlockChainExplorers() {
        if (bitcoinNetwork == BitcoinNetwork.MAINNET)
            return blockChainExplorersMainNet;
        else
            return blockChainExplorersTestNet;
    }

    public boolean getShowPlaceOfferConfirmation() {
        return showPlaceOfferConfirmation;
    }


    public boolean getShowTakeOfferConfirmation() {
        return showTakeOfferConfirmation;
    }

    public String getBackupDirectory() {
        return backupDirectory;
    }

    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory = backupDirectory;
        storage.queueUpForSave();
    }

    public void setAutoSelectArbitrators(boolean autoSelectArbitrators) {
        this.autoSelectArbitrators = autoSelectArbitrators;
        storage.queueUpForSave();
    }

    public boolean getAutoSelectArbitrators() {
        return autoSelectArbitrators;
    }

    public Map<String, Boolean> getShowAgainMap() {
        return showAgainMap;
    }

    public boolean getTacAccepted() {
        return tacAccepted;
    }
}
