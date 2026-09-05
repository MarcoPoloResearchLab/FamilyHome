import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const portalNavigation = MethodChannel('familyhome/navigation');

class PortalToolbar extends StatelessWidget implements PreferredSizeWidget {
  final Widget title;
  final List<Widget> actions;
  const PortalToolbar({super.key, required this.title, this.actions = const []});
  @override
  Size get preferredSize => const Size.fromHeight(64);
  @override
  Widget build(BuildContext context) => AppBar(
    toolbarHeight: 64,
    leadingWidth: 128,
    leading: Row(children: [
      IconButton(tooltip: 'Back', icon: const Icon(Icons.arrow_back),
        constraints: const BoxConstraints.tightFor(width: 56, height: 56),
        onPressed: () => WidgetsBinding.instance.handlePopRoute()),
      IconButton(tooltip: 'Home', icon: const Icon(Icons.home_outlined),
        constraints: const BoxConstraints.tightFor(width: 56, height: 56),
        onPressed: () => portalNavigation.invokeMethod<void>('home')),
    ]),
    title: title,
    actions: actions,
  );
}
