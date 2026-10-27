# Thermo-Print-Studio
================================================================================
                  THERMOPRINT STUDIO - FULL FUNCTIONAL DESCRIPTION
         Professional Medical & Diagnostic Thermal Receipt Printing Suite
================================================================================

OVERVIEW
--------
ThermoPrint Studio is an all-in-one, medical-grade thermal printing solution 
engineered for clinics, diagnostic centers, pharmacies, point-of-care stations, 
and mobile healthcare professionals. Built with a fast, modern interface, it 
transforms your Android smartphone or tablet into a complete diagnostic slip 
workstation—allowing you to generate, preview, and print crisp 58mm thermal 
receipts over Bluetooth, Wi-Fi, or system PDF in seconds.


KEY HIGHLIGHTS & CORE FEATURES
------------------------------

1. UNIVERSAL PRINTER CONNECTIVITY (BLUETOOTH, BLE & WI-FI)
   • Bluetooth Classic (SPP): Effortlessly connect to standard paired POS and 
     portable thermal printers with automatic RFCOMM socket fallback for 
     maximum hardware compatibility.
   • Bluetooth Low Energy (BLE): Built-in GATT service and characteristic 
     auto-discovery supports modern low-energy portable thermal printers.
   • Wi-Fi / LAN Network Printing (RAW TCP/IP): Print directly over your clinic 
     network via Port 9100 (or custom ports) with high-speed socket streaming.
   • System Print & PDF Export (Mopria / AirPrint): No thermal printer nearby? 
     Generate high-resolution PDF slips or print to standard office printers 
     using Android’s native Print Framework.

2. SMART PRINTER SEARCH & LIVE CONNECTION DASHBOARD
   • Instant Bluetooth Search & Filtering: Quickly filter paired or nearby 
     Bluetooth printers by device name or MAC address, or trigger a live 
     10-second proximity scan.
   • Automated Wi-Fi Subnet Scanner: Automatically sweep your local Wi-Fi 
     network to discover active thermal printers on Port 9100, or use 
     one-tap Quick IP selection chips.
   • Real-Time Active Printer Status Banner: Always know which printer is 
     connected at a glance with live status indicators, connection protocol 
     badges, MAC/IP address display, and one-tap disconnect controls.
   • Built-in Diagnostic Console: View real-time ESC/POS transmission logs and 
     run hardware self-test prints to verify printer alignment and speed.

3. TAILORED FOR MEDICAL DIAGNOSTICS & POINT-OF-CARE
   • Practice & Clinic Branding: Customize your clinic name, address, phone 
     number, and registration header with automatic local persistence.
   • Dedicated Vital & Lab Test Modules:
     - Random Blood Glucose (RBS) in mg/dL
     - Glycated Hemoglobin (HbA1c) in %
     - Blood Pressure (Systolic / Diastolic) in mmHg
   • Automatic Live Timestamping: Real-time date and clock synchronization 
     ensures every patient slip carries an accurate diagnostic timestamp.

4. STUDIO-GRADE LOGO DITHERING ENGINE
   • Floyd-Steinberg 1-Bit Monochrome Dithering: Upload any clinic logo or 
     emblem from your device gallery—or use the built-in medical caduceus 
     preset—and watch it convert into a sharp, thermal-ready 1-bit raster 
     graphic (ESC/POS GS v 0 standard).
   • Side-by-Side Image Preview: Inspect both your original artwork and the 
     dithered thermal output before printing a single millimeter of paper.

5. INTERACTIVE WYSIWYG 58MM LIVE RECEIPT PREVIEW
   • What You See Is What You Print: Every keystroke updates a realistic 58mm 
     (384-dot width) thermal paper preview complete with perforated锯tooth 
     edges, monospace typography, and dithered header graphics.
   • Adaptive Layout for Phones & Tablets: Enjoy a streamlined vertical 
     workflow on smartphones or a side-by-side split workstation view on 
     tablets and foldables.

6. COMPLETE PRINT HISTORY & ONE-TAP REPRINTING
   • Local Receipt Archive: Automatically logs up to 50 recent print jobs 
     locally on your device for complete privacy and offline reliability.
   • Instant Reload & Reprint: Browse past patient slips in the History Sheet 
     to reload values into the editor with a single tap, or manage and clear 
     records anytime.

7. ERGONOMIC DARK & LIGHT THEMES
   • Instant Theme Switching: Toggle seamlessly between a high-contrast Light 
     Mode for bright clinical environments and a sleek Dark Mode that reduces 
     eye strain during night shifts.


TECHNICAL SPECIFICATIONS
------------------------
• Paper Width Standard : 58mm Thermal Paper (384 dots/line, 32 chars/line)
• Command Protocol     : Standard ESC/POS (Initialize, Raster Bit-Image, Cut)
• Supported Transports : Bluetooth Classic SPP, Bluetooth LE (GATT), 
                         Wi-Fi TCP/IP (Port 9100), Android PrintManager / PDF
• Offline Capability   : 100% Offline-First Operation (No cloud account required)


WHY CHOOSE THERMOPRINT STUDIO?
------------------------------
• Zero Setup Friction: Connect, preview, and print in under 10 seconds.
• Hardware Agnostic: Works with virtually any ESC/POS-compatible 58mm printer.
• Professional Patient Experience: Deliver clean, branded, easy-to-read 
  diagnostic slips that build patient trust.
================================================================================
