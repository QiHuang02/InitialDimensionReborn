package cn.qihuang02.initial_dimension.event;

import cn.qihuang02.initial_dimension.InitialDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

@EventBusSubscriber(modid = InitialDimension.MODID, value = Dist.CLIENT)
public class WorldCreationGuiExtension {
    
    private static String selectedDimension = "minecraft:overworld";
    private static final String DIMENSION_CONFIG_FILE = "temp_initial_dimension_setting.txt";
    
    private static CycleButton<DimensionRegistry.DimensionInfo> dimensionButton;
    
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.@NotNull Post event) {
        if (event.getScreen() instanceof CreateWorldScreen createWorldScreen) {
            // Ensure dimension registry is initialized
            DimensionRegistry.forceRescan();
            addDimensionButton(event, createWorldScreen);
        }
    }
    
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.@NotNull Post event) {
        if (event.getScreen() instanceof CreateWorldScreen createWorldScreen && dimensionButton != null) {
            // Check tab visibility
            try {
                Field tabManagerField = CreateWorldScreen.class.getDeclaredField("tabManager");
                tabManagerField.setAccessible(true);
                Object tabManager = tabManagerField.get(createWorldScreen);
                
                boolean shouldShow = isCurrentlyWorldTab(tabManager);
                dimensionButton.visible = shouldShow;
                
                // If button is visible, check if layout needs updating (responsive to window size changes)
                if (shouldShow) {
                    updateButtonLayoutIfNeeded(createWorldScreen);
                }
                
            } catch (Exception e) {
                // If detection fails, default to show (at least ensure functionality is available)
                dimensionButton.visible = true;
            }
        }
    }
    
    private static int lastScreenWidth = -1;
    private static int lastScreenHeight = -1;
    
    /**
     * Check if button layout needs updating (when window size changes)
     */
    private static void updateButtonLayoutIfNeeded(CreateWorldScreen createWorldScreen) {
        int currentWidth = createWorldScreen.width;
        int currentHeight = createWorldScreen.height;
        
        // Check if window size has changed
        if (lastScreenWidth != currentWidth || lastScreenHeight != currentHeight) {
            lastScreenWidth = currentWidth;
            lastScreenHeight = currentHeight;
            
            // Recalculate layout
            ButtonLayout newLayout = calculateWorldTabLayout(createWorldScreen);
            
            // Update button position and size
            dimensionButton.setX(newLayout.x);
            dimensionButton.setY(newLayout.y);
            dimensionButton.setWidth(newLayout.width);
            dimensionButton.setHeight(newLayout.height);
            
            InitialDimension.LOGGER.debug("Updated dimension button layout: X={}, Y={}, W={}, H={}", 
                newLayout.x, newLayout.y, newLayout.width, newLayout.height);
        }
    }
    
    private static boolean isCurrentlyWorldTab(Object tabManager) {
        try {
            // Try to get current tab information through reflection
            Field[] fields = tabManager.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(tabManager);
                
                // Look for fields that might represent the current tab
                if (value != null && value.getClass().getSimpleName().contains("Tab")) {
                    String tabClassName = value.getClass().getSimpleName();
                    return tabClassName.equals("WorldTab");
                }
            }
            
            // Fallback method: assume World tab is the second one (index 1)
            return true; // Simplified: always show for now
        } catch (Exception e) {
            return true; // Default to show when error occurs
        }
    }
    
    private static void addDimensionButton(ScreenEvent.Init.Post event, CreateWorldScreen createWorldScreen) {
        // Get dynamic dimension list
        List<DimensionRegistry.DimensionInfo> availableDimensions = DimensionRegistry.getAvailableDimensions();
        
        if (availableDimensions.isEmpty()) {
            InitialDimension.LOGGER.warn("No available dimensions, skipping dimension selector addition");
            return;
        }
        
        // Calculate button layout, strictly following WorldTab's layout approach
        ButtonLayout layout = calculateWorldTabLayout(createWorldScreen);
        
        // Create dimension selector button using same size specifications as WorldTab
        dimensionButton = CycleButton.<DimensionRegistry.DimensionInfo>builder(
            dimension -> Component.literal(dimension.getDisplayName())
        )
        .withValues(availableDimensions)
        .withInitialValue(getDimensionInfoByKey(selectedDimension, availableDimensions))
        .create(
            layout.x,       // Dynamically calculated X position (center aligned)
            layout.y,       // Dynamically calculated Y position (below existing components)
            layout.width,   // Width - completely consistent with other WorldTab config items (210px)
            layout.height,  // Height - standard button height (20px)
            Component.translatable("gui.initial_dimension.spawn_dimension_label"),  // Button title
            (button, dimension) -> {
                selectedDimension = dimension.getKey();
                InitialDimension.LOGGER.info("User selected initial dimension: {} ({})", dimension.getDisplayName(), selectedDimension);
            }
        );
        
        // Add button to event
        event.addListener(dimensionButton);
        
        InitialDimension.LOGGER.info("Added dimension selector button, layout: X={}, Y={}, W={}, H={}, available dimensions: {}", 
            layout.x, layout.y, layout.width, layout.height, availableDimensions.size());
    }
    
    /**
     * Dynamically calculate button position and size, strictly following WorldTab's layout approach
     */
    private static ButtonLayout calculateWorldTabLayout(CreateWorldScreen createWorldScreen) {
        // WorldTab uses GridLayout.RowHelper to manage component layout
        // From source code: WorldTab uses columnSpacing(10).rowSpacing(8)
        // Component width is fixed at 210px (visible from .width(210).build() in source code)
        
        int screenWidth = createWorldScreen.width;
        int screenHeight = createWorldScreen.height;
        
        // Get tab area information
        int headerHeight = getTabHeaderHeight(createWorldScreen);
        int footerHeight = getFooterHeight(createWorldScreen);
        
        // Tab content area calculation
        int tabAreaTop = headerHeight;
        int tabAreaHeight = screenHeight - headerHeight - footerHeight;
        int tabAreaCenterY = tabAreaTop + tabAreaHeight / 2;
        
        // WorldTab layout parameters (derived from source code analysis)
        final int COMPONENT_WIDTH = 210;  // Standard width of components in WorldTab
        final int COMPONENT_HEIGHT = 20;  // Standard button height
        final int ROW_SPACING = 8;        // Row spacing
        
        // Calculate X position (center aligned)
        int componentX = (screenWidth - COMPONENT_WIDTH) / 2;
        
        // Precisely calculate component layout based on WorldTab source code
        // WorldTab component order: World type selector + custom button (2 rows) → Seed input field (2 rows) → SwitchGrid (2 rows)
        // Total approximately 6-7 rows of components, each row height 20px + row spacing 8px
        
        // Basic component count analysis:
        // 1. World type selector (CycleButton) - 1 row
        // 2. Custom type button (Button) - 1 row  
        // 3. Seed label + input field - 2 rows
        // 4. Structure generation switch - 1 row
        // 5. Bonus chest switch - 1 row
        // Total approximately 6 rows
        
        int totalRows = 6;
        int singleRowHeight = COMPONENT_HEIGHT + ROW_SPACING;
        int totalUsedHeight = totalRows * singleRowHeight - ROW_SPACING; // Last row doesn't need row spacing
        
        // Calculate starting Y position (vertical center aligned)
        int contentStartY = tabAreaCenterY - totalUsedHeight / 2;
        
        // Our button should be placed after the last row
        int ourButtonY = contentStartY + totalUsedHeight + ROW_SPACING;
        
        // Boundary check: ensure button doesn't exceed visible area
        int minY = tabAreaTop + 20; // At least 20 pixels from top
        int maxY = tabAreaTop + tabAreaHeight - COMPONENT_HEIGHT - 20; // At least 20 pixels from bottom
        
        if (ourButtonY < minY) {
            ourButtonY = minY;
        } else if (ourButtonY > maxY) {
            ourButtonY = maxY;
        }
        
        return new ButtonLayout(componentX, ourButtonY, COMPONENT_WIDTH, COMPONENT_HEIGHT);
    }
    
    /**
     * Button layout information class
     */
    private static class ButtonLayout {
        final int x, y, width, height;
        
        ButtonLayout(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * Get tab header height
     */
    private static int getTabHeaderHeight(CreateWorldScreen createWorldScreen) {
        try {
            // Try to get TabNavigationBar height
            Field tabNavBarField = CreateWorldScreen.class.getDeclaredField("tabNavigationBar");
            tabNavBarField.setAccessible(true);
            Object tabNavigationBar = tabNavBarField.get(createWorldScreen);
            
            if (tabNavigationBar != null) {
                // Try to get TabNavigationBar's Rectangle
                Field rectangleField = null;
                for (Field field : tabNavigationBar.getClass().getDeclaredFields()) {
                    if (field.getType().getSimpleName().contains("Rectangle") || 
                        field.getType().getSimpleName().contains("Rect")) {
                        rectangleField = field;
                        break;
                    }
                }
                
                if (rectangleField != null) {
                    rectangleField.setAccessible(true);
                    Object rectangle = rectangleField.get(tabNavigationBar);
                    
                    // Try to get bottom value
                    try {
                        var bottomMethod = rectangle.getClass().getMethod("bottom");
                        return (Integer) bottomMethod.invoke(rectangle);
                    } catch (Exception e) {
                        // If method call fails, use field
                        try {
                            Field bottomField = rectangle.getClass().getDeclaredField("bottom");
                            bottomField.setAccessible(true);
                            return (Integer) bottomField.get(rectangle);
                        } catch (Exception e2) {
                            // Continue trying other methods
                        }
                    }
                }
            }
        } catch (Exception e) {
            InitialDimension.LOGGER.debug("Unable to get TabNavigationBar height: {}", e.getMessage());
        }
        
        // Default estimated value: tab header is usually about 40 pixels high
        return createWorldScreen.height / 6;
    }
    
    /**
     * Get footer height
     */
    private static int getFooterHeight(CreateWorldScreen createWorldScreen) {
        try {
            // Try to get HeaderAndFooterLayout
            Field layoutField = CreateWorldScreen.class.getDeclaredField("layout");
            layoutField.setAccessible(true);
            Object layout = layoutField.get(createWorldScreen);
            
            if (layout != null) {
                // Try to call getFooterHeight method
                var getFooterHeightMethod = layout.getClass().getMethod("getFooterHeight");
                return (Integer) getFooterHeightMethod.invoke(layout);
            }
        } catch (Exception e) {
            InitialDimension.LOGGER.debug("Unable to get footer height: {}", e.getMessage());
        }
        
        // Default estimated value: footer usually contains buttons, about 40-50 pixels high
        return 50;
    }
    
    
    /**
     * Get dimension info object by dimension key
     */
    private static DimensionRegistry.DimensionInfo getDimensionInfoByKey(String key, List<DimensionRegistry.DimensionInfo> dimensions) {
        return dimensions.stream()
            .filter(dimension -> dimension.getKey().equals(key))
            .findFirst()
            .orElse(dimensions.isEmpty() ? null : dimensions.get(0));
    }
    
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        // Save selected dimension settings when leaving world creation screen
        if (!(event.getNewScreen() instanceof CreateWorldScreen) && 
            event.getCurrentScreen() instanceof CreateWorldScreen) {
            saveDimensionToWorldFolder();
        }
    }
    
    private static void saveDimensionToWorldFolder() {
        try {
            // Get current Minecraft instance
            Minecraft minecraft = Minecraft.getInstance();
            File gameDir = minecraft.gameDirectory;
            File savesDir = new File(gameDir, "saves");
            
            // Create a temporary file to store dimension selection
            // Note: actual world folder name needs to be determined when world is created
            File tempConfigFile = new File(gameDir, DIMENSION_CONFIG_FILE);
            
            try (FileWriter writer = new FileWriter(tempConfigFile)) {
                writer.write(selectedDimension);
                InitialDimension.LOGGER.info("Saved temporary dimension setting: {}", selectedDimension);
            }
            
        } catch (IOException e) {
            InitialDimension.LOGGER.error("Failed to save dimension setting", e);
        }
    }
    
    public static String getSelectedDimension() {
        return selectedDimension;
    }
    
    public static void setSelectedDimension(String dimension) {
        selectedDimension = dimension;
    }
    
}