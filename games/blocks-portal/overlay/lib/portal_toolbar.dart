import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'portal_theme.dart';

const portalNavigation = MethodChannel('familyhome/navigation');

class PortalToolbar extends StatelessWidget implements PreferredSizeWidget {
  final Widget title;
  final List<Widget> actions;
  const PortalToolbar({super.key, required this.title, this.actions = const []});
  @override
  Size get preferredSize => const Size.fromHeight(72);
  @override
  Widget build(BuildContext context) => AppBar(
    toolbarHeight: 72,
    leadingWidth: 144,
    leading: Row(children: [
      IconButton(tooltip: 'Back', icon: const Icon(Icons.arrow_back),
        constraints: const BoxConstraints.tightFor(width: portalControlHeight, height: portalControlHeight),
        onPressed: () => WidgetsBinding.instance.handlePopRoute()),
      IconButton(tooltip: 'Home', icon: const Icon(Icons.home_outlined),
        constraints: const BoxConstraints.tightFor(width: portalControlHeight, height: portalControlHeight),
        onPressed: () => portalNavigation.invokeMethod<void>('home')),
    ]),
    title: DefaultTextStyle(style: portalControlText, child: title),
    actions: actions,
  );
}
