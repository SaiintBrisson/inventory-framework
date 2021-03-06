package me.saiintbrisson.minecraft;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public class View extends VirtualView implements InventoryHolder, Closeable {

    public static final int INVENTORY_ROW_SIZE = 9;
    public static final int UNSET_SLOT = -1;

    private ViewFrame frame;
    private final String title;
    private final int rows;
    private final Map<Player, ViewContext> contexts;
    private boolean cancelOnClick, cancelOnPickup, cancelOnDrop, cancelOnDrag, cancelOnClone, cancelOnMoveOut;
    private final Map<Player, Map<String, Object>> data;

    public View(final int rows) {
        this(null, rows, "");
    }

    public View(final int rows, final String title) {
        this(null, rows, title);
    }

    public View(final ViewFrame frame, final int rows, final String title) {
        super(new ViewItem[INVENTORY_ROW_SIZE * rows]);
        this.rows = rows;
        this.frame = frame;
        this.title = title;
        contexts = new WeakHashMap<>();
        data = new WeakHashMap<>();
        cancelOnPickup = true;
        cancelOnDrop = true;
        cancelOnDrag = true;
        cancelOnClone = true;
        cancelOnMoveOut = true;
    }

    @Override
    public int getLastSlot() {
        return INVENTORY_ROW_SIZE * rows - 1;
    }

    public Map<Player, ViewContext> getContexts() {
        return contexts;
    }

    public ViewContext getContext(final Player player) {
        return contexts.get(player);
    }

    public ViewFrame getFrame() {
        return frame;
    }

    void setFrame(final ViewFrame frame) {
        this.frame = frame;
    }

    public int getRows() {
        return rows;
    }

    public String getTitle() {
        return title;
    }

    protected ViewContext createContext(final View view, final Player player, final Inventory inventory) {
        return new ViewContext(view, player, inventory);
    }

    public void open(final Player player) {
        open(player, null);
    }

    public void open(final Player player, final Map<String, Object> data) {
        if (contexts.containsKey(player))
            return;

        final OpenViewContext preOpenContext = new OpenViewContext(this, player);
        if (data != null)
            setData(player, new HashMap<>(data));
        else {
            // ensure non-transitive data on view switch
            clearData(player);
        }

        onOpen(preOpenContext);
        if (preOpenContext.isCancelled()) {
            clearData(player);
            return;
        }

        final Inventory inventory = getInventory(preOpenContext.getInventoryTitle(), preOpenContext.getInventorySize());
        final ViewContext context = createContext(this, player, inventory);
        contexts.put(player, context);
        onRender(context);
        render(context);
        player.openInventory(inventory);
    }

    @Override
    public void update(final ViewContext context) {
        frame.debug("[context]: update");
        onUpdate(context);
        super.update(context);
    }

    @Override
    public void update(final ViewContext context, final int slot) {
        frame.debug("[slot " + slot + "]: update");
        super.update(context, slot);
    }

    @Override
    public void render(final ViewContext context) {
        frame.debug("[context]: render");
        super.render(context);
    }

    @Override
    public void render(final ViewContext context, final int slot) {
        frame.debug("[slot " + slot + "]: render");
        super.render(context, slot);
    }

    @Override
    public void render(final ViewContext context, final ViewItem item, final int slot) {
        frame.debug("[slot " + slot + "]: render with item");
        super.render(context, item, slot);
    }

    @Override
    ViewItem resolve(final ViewContext context, final int slot) {
        frame.debug("[slot " + slot + "]: resolve item");
        return super.resolve(context, slot);
    }

    ViewContext remove(final Player player) {
        frame.debug("[context]: remove");
        final ViewContext context = contexts.remove(player);
        if (context != null) {
            context.invalidate();
            frame.debug("[context]: invalidate");
        }

        return context;
    }

    public void close() {
        for (final Player player : contexts.keySet()) {
            player.closeInventory();
        }
    }

    public boolean isCancelOnClick() {
        return cancelOnClick;
    }

    public void setCancelOnClick(final boolean cancelOnClick) {
        this.cancelOnClick = cancelOnClick;
    }

    public boolean isCancelOnPickup() {
        return cancelOnPickup;
    }

    public void setCancelOnPickup(final boolean cancelOnPickup) {
        this.cancelOnPickup = cancelOnPickup;
    }

    public boolean isCancelOnDrop() {
        return cancelOnDrop;
    }

    public void setCancelOnDrop(final boolean cancelOnDrop) {
        this.cancelOnDrop = cancelOnDrop;
    }

    public boolean isCancelOnDrag() {
        return cancelOnDrag;
    }

    public void setCancelOnDrag(final boolean cancelOnDrag) {
        this.cancelOnDrag = cancelOnDrag;
    }

    public boolean isCancelOnClone() {
        return cancelOnClone;
    }

    public void setCancelOnClone(final boolean cancelOnClone) {
        this.cancelOnClone = cancelOnClone;
    }

    public boolean isCancelOnMoveOut() {
        return cancelOnMoveOut;
    }

    public void setCancelOnMoveOut(boolean cancelOnMoveOut) {
        this.cancelOnMoveOut = cancelOnMoveOut;
    }

    @Override
    public Inventory getInventory() {
        throw new UnsupportedOperationException();
    }

    private Inventory getInventory(final String title, final int size) {
        return Bukkit.createInventory(this, size, title == null ? this.title : title);
    }

    public void clearData(final Player player) {
        data.remove(player);
    }

    public void clearData(final Player player, final String key) {
        if (!data.containsKey(player))
            return;

        data.get(player).remove(key);
    }

    public Map<String, Object> getData(final Player player) {
        return data.get(player);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(final Player player, final String key) {
        if (!data.containsKey(player))
            return null;

        return (T) data.get(player).get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(final Player player, final String key, final Supplier<T> defaultValue) {
        if (!data.containsKey(player) || !data.get(player).containsKey(key))
            return defaultValue.get();

        return (T) data.get(player).get(key);
    }

    public void setData(final Player player, final Map<String, Object> data) {
        this.data.put(player, data);
    }

    public void setData(final Player player, final String key, final Object value) {
        data.computeIfAbsent(player, $ -> new HashMap<>()).put(key, value);
    }

    public boolean hasData(final Player player, final String key) {
        if (!data.containsKey(player))
            return false;

        return data.get(player).containsKey(key);
    }

    protected void onOpen(final OpenViewContext context) {
    }

    protected void onRender(final ViewContext context) {
    }

    protected void onClose(final ViewContext context) {
    }

    protected void onClick(final ViewSlotContext context) {
    }

    protected void onUpdate(final ViewContext context) {
    }

    protected void onMoveOut(final ViewSlotMoveContext context) {
    }

}