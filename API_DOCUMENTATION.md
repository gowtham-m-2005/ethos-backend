# Ethos Delivery Backend API Documentation

## Base URL
```
http://localhost:8080
```

## Content Type
```
Content-Type: application/json
```

---

## 📦 Package Management Endpoints

### 1. Get All Packages
**Endpoint:** `GET /api/packages`

**Description:** Retrieves all packages sorted by ethical score (highest first)

**Response:** Array of PackagePriority objects

**Example Response:**
```json
[
  {
    "id": 1,
    "packageId": "PKG-1770225988626",
    "currentPriority": 1,
    "deliveryType": "MEDICAL_EXPRESS",
    "pickupLocation": "City Medical Center",
    "destination": "General Hospital, Emergency Ward",
    "deliveryTime": "2026-02-05T08:00:00",
    "ethicalScore": 9.0,
    "pythonResponse": "{...}",
    "createdAt": "2026-02-04T22:56:28.6261985"
  }
]
```

---

### 2. Get Packages by Delivery Type
**Endpoint:** `GET /api/packages/delivery-type/{type}`

**Description:** Filters packages by their delivery type (domain)

**Path Parameters:**
- `type` (String): Delivery type (MEDICAL_EXPRESS, FOOD_EXPRESS, ESSENTIAL, FRAGILE, STANDARD, etc.)

**Example Requests:**
```
GET /api/packages/delivery-type/MEDICAL_EXPRESS
GET /api/packages/delivery-type/FOOD_EXPRESS
GET /api/packages/delivery-type/ESSENTIAL
```

**Response:** Array of PackagePriority objects with matching delivery type

---

### 3. Get Package Statistics
**Endpoint:** `GET /api/packages/stats`

**Description:** Returns comprehensive analytics about all packages

**Response:** Statistics object

**Example Response:**
```json
{
  "totalPackages": 15,
  "priorityDistribution": {
    "1": 3,
    "2": 5,
    "3": 4,
    "4": 3
  },
  "deliveryTypeDistribution": {
    "MEDICAL_EXPRESS": 4,
    "FOOD_EXPRESS": 3,
    "ESSENTIAL": 2,
    "STANDARD": 6
  },
  "averageEthicalScore": 4.7,
  "highestPriorityPackage": {...},
  "lowestPriorityPackage": {...}
}
```

---

### 4. Get Package by ID
**Endpoint:** `GET /api/packages/{id}`

**Description:** Retrieves a specific package by its database ID

**Path Parameters:**
- `id` (Long): Database ID of the package

**Response:** Single PackagePriority object or 404 Not Found

---

## 🤖 Ethos AI Analysis Endpoint

### 5. Analyze Package (Main Endpoint)
**Endpoint:** `POST /api/ethos`

**Description:** Analyzes a package using Python AI service for ethical scoring and priority assignment

**Request Body:** EthosModel object

**Example Request:**
```json
{
  "senderName": "Dr. Sarah Johnson",
  "pickupLocation": "City Medical Center",
  "packageDescription": "Emergency medical supplies - insulin, syringes, and prescription medications",
  "packageWeight": "1.5kg",
  "deliveryTime": "2026-02-05T08:00:00",
  "receiverName": "Hospital Pharmacy",
  "destination": "General Hospital, Emergency Ward"
}
```

**Response:** Python AI analysis result

**Example Response:**
```json
{
  "priority_level": 1,
  "requires_approval": true,
  "explanation": "DECISION EXPLANATION:\nPackage contained keywords (insulin, prescription) associated with the MEDICAL_EXPRESS domain.\nRisk Profile assessed as:\n  - Harm Potential: 9/10 (Weight: 3.6)\n  - Vulnerability: 9/10 (Weight: 3.1)\n  - Time Sensitivity: 9/10 (Weight: 2.2)\nTotal Ethical Score: 9.00\nCONCLUSION: CRITICAL PRIORITY assigned due to high ethical risk score indicating potential severe harm or vulnerability.\n[SAFETY INTERVENTION]: Human approval required for Priority 1 dispatch.",
  "score": {
    "harm_score": 3.6,
    "vulnerability_score": 3.15,
    "time_score": 2.25,
    "total_score": 9.0
  }
}
```

**Side Effects:**
- Stores package information in database
- Extracts domain from AI response
- Calculates priority based on ethical score
- Recalculates all package priorities (relative ranking)
- Console logging shows domain extraction and priority changes

---

## 📊 Data Models

### PackagePriority Object
```json
{
  "id": 1,
  "packageId": "PKG-1770225988626",
  "currentPriority": 1,
  "deliveryType": "MEDICAL_EXPRESS",
  "pickupLocation": "City Medical Center",
  "destination": "General Hospital, Emergency Ward",
  "deliveryTime": "2026-02-05T08:00:00",
  "ethicalScore": 9.0,
  "pythonResponse": "{...}",
  "createdAt": "2026-02-04T22:56:28.6261985"
}
```

### EthosModel Object (Request)
```json
{
  "senderName": "string",
  "pickupLocation": "string",
  "packageDescription": "string",
  "packageWeight": "string",
  "deliveryTime": "string",
  "receiverName": "string",
  "destination": "string"
}
```

---

## 🎯 Priority System

### Relative Priority Ranking
- **Priority 1:** Highest ethical score
- **Priority 2:** Second highest ethical score
- **Priority 3:** Third highest ethical score
- **Priority 4:** Lowest ethical score

**Note:** Priorities are recalculated automatically whenever a new package is added to maintain relative ranking based on ethical scores.

### Ethical Score Ranges
- **8.0 - 10.0:** Critical Priority (typically medical emergencies)
- **6.0 - 7.9:** High Priority (essential items)
- **3.0 - 5.9:** Medium Priority (food, fragile items)
- **0.0 - 2.9:** Low Priority (standard goods)

---

## 🏷️ Delivery Types (Domains)

Delivery types are extracted from Python AI response using the pattern:
`"associated with the [DOMAIN] domain."`

**Common Delivery Types:**
- `MEDICAL_EXPRESS` - Medical supplies, medications
- `FOOD_EXPRESS` - Perishable food items
- `ESSENTIAL` - Essential supplies and equipment
- `FRAGILE` - Delicate or breakable items
- `HEAVY` - Heavy or bulky items
- `STANDARD` - Regular packages

---

## 🔧 Error Responses

### 404 Not Found
```json
{
  "timestamp": "2026-02-04T22:56:28.6261985",
  "status": 404,
  "error": "Not Found",
  "message": "Package not found",
  "path": "/api/packages/999"
}
```

### 400 Bad Request
```json
{
  "timestamp": "2026-02-04T22:56:28.6261985",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request format",
  "path": "/api/ethos"
}
```

---

## 🌐 CORS Configuration
- **Allowed Origins:** `*` (all origins)
- **Allowed Methods:** GET, POST, PUT, DELETE
- **Allowed Headers:** All headers

---

## 📝 Usage Examples

### Example 1: Submit Medical Package
```bash
curl -X POST http://localhost:8080/api/ethos \
  -H "Content-Type: application/json" \
  -d '{
    "senderName": "Emergency Medical Services",
    "pickupLocation": "Central Hospital",
    "packageDescription": "CRITICAL - Life-saving organ transplant package",
    "packageWeight": "5kg",
    "deliveryTime": "2026-02-05T02:00:00",
    "receiverName": "Transplant Team",
    "destination": "City General Hospital"
  }'
```

### Example 2: Get All Medical Packages
```bash
curl -X GET http://localhost:8080/api/packages/delivery-type/MEDICAL_EXPRESS
```

### Example 3: Get Package Statistics
```bash
curl -X GET http://localhost:8080/api/packages/stats
```

---

## 🚀 Quick Start Guide

1. **Start the application:** Run the Spring Boot application
2. **Submit a package:** Use `POST /api/ethos` with package details
3. **Check console:** Monitor domain extraction and priority assignment
4. **View results:** Use `GET /api/packages` to see all packages
5. **Get analytics:** Use `GET /api/packages/stats` for insights

---

*Last Updated: February 4, 2026*
