import 'package:flutter/material.dart';

const portalPaper = Color(0xfffffbef);
const portalInk = Colors.black;
const portalYellow = Color(0xffffd65c);
const portalMint = Color(0xff77ddb5);
const portalPurple = Color(0xffc4aaff);
const portalControlHeight = 60.0;
const portalBodySize = 20.0;

const portalControlText = TextStyle(fontFamily: 'Fredoka', fontWeight: FontWeight.w700, fontSize: 22, color: portalInk);
const portalTitleText = TextStyle(fontFamily: 'Fredoka', fontWeight: FontWeight.w700, fontSize: 32, color: portalInk);
const portalBodyText = TextStyle(fontSize: portalBodySize, fontWeight: FontWeight.w500, color: portalInk);
const portalRadius = BorderRadius.all(Radius.circular(16));
final portalBorder = RoundedRectangleBorder(borderRadius: portalRadius, side: const BorderSide(color: portalInk, width: 3));

class _ButtonShadow extends CustomPainter {
  final double depth;
  const _ButtonShadow(this.depth);

  @override
  void paint(Canvas canvas, Size size) {
    final outline = RRect.fromRectAndRadius(Offset.zero & size, const Radius.circular(16));
    final face = outline.shift(Offset(-depth, -depth));
    final shadow = Path.combine(PathOperation.difference,
      Path()..addRRect(outline), Path()..addRRect(face));
    canvas.drawPath(shadow, Paint()..color = portalInk);
  }

  @override
  bool shouldRepaint(_ButtonShadow oldDelegate) => depth != oldDelegate.depth;
}

ThemeData portalTheme() {
  final action = ButtonStyle(
    foregroundColor: const WidgetStatePropertyAll(portalInk),
    backgroundColor: WidgetStateProperty.resolveWith((states) => states.contains(WidgetState.disabled) ? const Color(0xffdeded8) : portalYellow),
    minimumSize: const WidgetStatePropertyAll(Size(60, portalControlHeight)),
    textStyle: const WidgetStatePropertyAll(portalControlText),
    backgroundBuilder: (context, states, child) => CustomPaint(
      painter: _ButtonShadow(states.contains(WidgetState.pressed) ? 1 : 4), child: child),
    shape: WidgetStatePropertyAll(portalBorder),
    side: const WidgetStatePropertyAll(BorderSide(color: portalInk, width: 3)),
  );
  return ThemeData(
    useMaterial3: true,
    scaffoldBackgroundColor: portalPaper,
    colorScheme: const ColorScheme.light(primary: portalInk, onPrimary: portalInk,
      secondary: portalMint, onSecondary: portalInk, surface: portalPaper, onSurface: portalInk,
      surfaceContainerHighest: Color(0xff8dd4ff), outline: portalInk),
    textTheme: const TextTheme(
      displayLarge: portalTitleText, displayMedium: portalTitleText, displaySmall: portalTitleText,
      headlineLarge: portalTitleText, headlineMedium: portalTitleText, headlineSmall: portalTitleText,
      titleLarge: portalTitleText, titleMedium: portalControlText, titleSmall: portalControlText,
      bodyLarge: portalBodyText, bodyMedium: portalBodyText, bodySmall: portalBodyText,
      labelLarge: portalControlText, labelMedium: portalControlText, labelSmall: portalControlText),
    appBarTheme: const AppBarTheme(backgroundColor: portalPaper, foregroundColor: portalInk,
      elevation: 0, titleTextStyle: portalTitleText, toolbarHeight: 72),
    elevatedButtonTheme: ElevatedButtonThemeData(style: action),
    filledButtonTheme: FilledButtonThemeData(style: action),
    outlinedButtonTheme: OutlinedButtonThemeData(style: action),
    textButtonTheme: TextButtonThemeData(style: action),
    iconButtonTheme: IconButtonThemeData(style: action),
    dialogTheme: DialogThemeData(backgroundColor: portalPaper, shape: portalBorder,
      titleTextStyle: portalTitleText, contentTextStyle: portalBodyText),
    inputDecorationTheme: InputDecorationTheme(border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: const BorderSide(color: portalInk, width: 3))),
  );
}
