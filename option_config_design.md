# Product Option Configuration Design

## JSON Structure

Each product in `dream_items` table will have an `option_config` field (JSON type) with this structure:

```json
{
  "options": [
    {
      "label": "法拉利",
      "suggestedAmounts": [1000000, 1500000, 2000000]
    },
    {
      "label": "兰博基尼",
      "suggestedAmounts": [1500000, 2000000, 3000000]
    },
    {
      "label": "特斯拉",
      "suggestedAmounts": [300000, 500000, 800000]
    }
  ]
}
```

## Product-Specific Configurations

### 轿车 (car)
```json
{
  "options": [
    {"label": "法拉利", "suggestedAmounts": [1000000, 1500000, 2000000]},
    {"label": "兰博基尼", "suggestedAmounts": [1500000, 2000000, 3000000]},
    {"label": "特斯拉", "suggestedAmounts": [300000, 500000, 800000]}
  ]
}
```

### SUV (suv)
```json
{
  "options": [
    {"label": "保时捷卡宴", "suggestedAmounts": [800000, 1200000, 1800000]},
    {"label": "路虎揽胜", "suggestedAmounts": [1000000, 1500000, 2000000]},
    {"label": "奔驰GLS", "suggestedAmounts": [900000, 1300000, 1700000]}
  ]
}
```

### 超跑 (supercar)
```json
{
  "options": [
    {"label": "布加迪", "suggestedAmounts": [20000000, 30000000, 50000000]},
    {"label": "迈凯伦", "suggestedAmounts": [3000000, 5000000, 8000000]},
    {"label": "兰博基尼Aventador", "suggestedAmounts": [5000000, 7000000, 10000000]}
  ]
}
```

### 包包 (bag)
```json
{
  "options": [
    {"label": "爱马仕Birkin", "suggestedAmounts": [100000, 200000, 500000]},
    {"label": "香奈儿Classic", "suggestedAmounts": [50000, 80000, 120000]},
    {"label": "LV", "suggestedAmounts": [20000, 40000, 80000]}
  ]
}
```

### 劳力士 (rolex)
```json
{
  "options": [
    {"label": "Submariner", "suggestedAmounts": [70000, 100000, 150000]},
    {"label": "Daytona", "suggestedAmounts": [150000, 250000, 400000]},
    {"label": "GMT-Master II", "suggestedAmounts": [80000, 120000, 180000]}
  ]
}
```

### 相机 (camera)
```json
{
  "options": [
    {"label": "徕卡M系列", "suggestedAmounts": [50000, 80000, 120000]},
    {"label": "哈苏X系列", "suggestedAmounts": [80000, 120000, 200000]},
    {"label": "索尼A1", "suggestedAmounts": [40000, 50000, 60000]}
  ]
}
```

## Frontend Usage

When a user selects a product (e.g., 轿车), the frontend will:
1. Display the options: "法拉利", "兰博基尼", "特斯拉"
2. When user selects "法拉利", display amounts: ¥1000000, ¥1500000, ¥2000000
3. When user selects "兰博基尼", display amounts: ¥1500000, ¥2000000, ¥3000000
4. When user selects "特斯拉", display amounts: ¥300000, ¥500000, ¥800000

## Database Changes

1. Add `option_config` column to `dream_items` table (JSON type)
2. Update `DreamItem.java` entity with `optionConfig` field (String type, will auto-serialize)
3. Frontend will parse the JSON and dynamically render options and amounts
