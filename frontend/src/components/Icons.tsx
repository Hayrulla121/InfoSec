import type { SVGProps } from 'react';

/**
 * Hand-rolled 24x24 stroke icons.
 *
 * Inline SVG rather than an icon package: nineteen icons is not worth a
 * dependency and a bundle, and `stroke="currentColor"` means every icon
 * inherits its colour from the surrounding text automatically.
 *
 * aria-hidden on all of them - each icon sits next to a visible text label,
 * so announcing it again would only add noise for screen readers.
 */
type IconProps = SVGProps<SVGSVGElement> & { size?: number };

function Icon({ size = 18, children, ...rest }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...rest}
    >
      {children}
    </svg>
  );
}

export const IconHome = (p: IconProps) => (
  <Icon {...p}>
    <path d="M3 10.5 12 3l9 7.5" />
    <path d="M5 9.5V20h14V9.5" />
    <path d="M9.5 20v-6h5v6" />
  </Icon>
);

export const IconBook = (p: IconProps) => (
  <Icon {...p}>
    <path d="M4 4.5A1.5 1.5 0 0 1 5.5 3H19v15H5.5A1.5 1.5 0 0 0 4 19.5z" />
    <path d="M4 19.5A1.5 1.5 0 0 1 5.5 18H19v3H5.5A1.5 1.5 0 0 1 4 19.5z" />
    <path d="M8 7.5h7M8 10.5h5" />
  </Icon>
);

export const IconDatabase = (p: IconProps) => (
  <Icon {...p}>
    <ellipse cx="12" cy="5.5" rx="7.5" ry="2.75" />
    <path d="M4.5 5.5v6c0 1.52 3.36 2.75 7.5 2.75s7.5-1.23 7.5-2.75v-6" />
    <path d="M4.5 11.5v6c0 1.52 3.36 2.75 7.5 2.75s7.5-1.23 7.5-2.75v-6" />
  </Icon>
);

export const IconAlert = (p: IconProps) => (
  <Icon {...p}>
    <path d="M10.6 3.8 2.5 18a1.6 1.6 0 0 0 1.4 2.4h16.2A1.6 1.6 0 0 0 21.5 18L13.4 3.8a1.6 1.6 0 0 0-2.8 0z" />
    <path d="M12 9v4.5M12 17h.01" />
  </Icon>
);

export const IconGrid = (p: IconProps) => (
  <Icon {...p}>
    <rect x="3" y="3" width="7.5" height="7.5" rx="1.2" />
    <rect x="13.5" y="3" width="7.5" height="7.5" rx="1.2" />
    <rect x="3" y="13.5" width="7.5" height="7.5" rx="1.2" />
    <rect x="13.5" y="13.5" width="7.5" height="7.5" rx="1.2" />
  </Icon>
);

export const IconClipboard = (p: IconProps) => (
  <Icon {...p}>
    <path d="M9 4h6a1 1 0 0 1 1 1v1H8V5a1 1 0 0 1 1-1z" />
    <path d="M8 6H6.5A1.5 1.5 0 0 0 5 7.5v12A1.5 1.5 0 0 0 6.5 21h11a1.5 1.5 0 0 0 1.5-1.5v-12A1.5 1.5 0 0 0 17.5 6H16" />
    <path d="M9 11h6M9 14.5h6M9 18h3.5" />
  </Icon>
);

export const IconShield = (p: IconProps) => (
  <Icon {...p}>
    <path d="M12 3l7 2.8v5.4c0 4.3-2.9 8.2-7 9.8-4.1-1.6-7-5.5-7-9.8V5.8z" />
    <path d="m9 12 2.2 2.2L15.5 10" />
  </Icon>
);

export const IconSliders = (p: IconProps) => (
  <Icon {...p}>
    <path d="M5 20v-6M5 10V4M12 20v-9M12 7V4M19 20v-4M19 12V4" />
    <path d="M2.5 14h5M9.5 7h5M16.5 16h5" />
  </Icon>
);

export const IconUsers = (p: IconProps) => (
  <Icon {...p}>
    <circle cx="9.5" cy="8" r="3.5" />
    <path d="M3 20c0-3.3 2.9-5.5 6.5-5.5S16 16.7 16 20" />
    <path d="M16.5 5.2a3.5 3.5 0 0 1 0 6.6M18 14.8c2.1.7 3.5 2.4 3.5 5.2" />
  </Icon>
);

export const IconDownload = (p: IconProps) => (
  <Icon {...p}>
    <path d="M12 3.5v11M7.5 10.5 12 15l4.5-4.5" />
    <path d="M4 17v2.5A1.5 1.5 0 0 0 5.5 21h13a1.5 1.5 0 0 0 1.5-1.5V17" />
  </Icon>
);

export const IconLogout = (p: IconProps) => (
  <Icon {...p}>
    <path d="M14 4.5h4A1.5 1.5 0 0 1 19.5 6v12a1.5 1.5 0 0 1-1.5 1.5h-4" />
    <path d="M10 16.5 14.5 12 10 7.5M14.5 12H3.5" />
  </Icon>
);

export const IconSearch = (p: IconProps) => (
  <Icon {...p}>
    <circle cx="10.5" cy="10.5" r="6.5" />
    <path d="m15.5 15.5 4.5 4.5" />
  </Icon>
);

export const IconPlus = (p: IconProps) => (
  <Icon {...p}>
    <path d="M12 5v14M5 12h14" />
  </Icon>
);

export const IconEdit = (p: IconProps) => (
  <Icon {...p}>
    <path d="M4 20h4L19 9a2.1 2.1 0 0 0-3-3L5 17z" />
    <path d="m14.5 6.5 3 3" />
  </Icon>
);

export const IconTrash = (p: IconProps) => (
  <Icon {...p}>
    <path d="M4 6.5h16M9.5 6.5V4.8A1 1 0 0 1 10.5 4h3a1 1 0 0 1 1 .8v1.7" />
    <path d="M6.5 6.5 7.4 20a1 1 0 0 0 1 .9h7.2a1 1 0 0 0 1-.9l.9-13.5" />
    <path d="M10.5 10.5v6M13.5 10.5v6" />
  </Icon>
);

export const IconClose = (p: IconProps) => (
  <Icon {...p}>
    <path d="M6 6l12 12M18 6 6 18" />
  </Icon>
);

export const IconChevronLeft = (p: IconProps) => (
  <Icon {...p}>
    <path d="M14.5 5.5 8 12l6.5 6.5" />
  </Icon>
);

export const IconChevronRight = (p: IconProps) => (
  <Icon {...p}>
    <path d="M9.5 5.5 16 12l-6.5 6.5" />
  </Icon>
);

export const IconLink = (p: IconProps) => (
  <Icon {...p}>
    <path d="M10 13.5a4 4 0 0 0 5.7 0l2.8-2.8a4 4 0 0 0-5.7-5.7l-1.4 1.4" />
    <path d="M14 10.5a4 4 0 0 0-5.7 0l-2.8 2.8a4 4 0 0 0 5.7 5.7l1.4-1.4" />
  </Icon>
);

export const IconInbox = (p: IconProps) => (
  <Icon {...p}>
    <path d="M3.5 13.5 6 5.2a1.5 1.5 0 0 1 1.4-1h9.2a1.5 1.5 0 0 1 1.4 1l2.5 8.3" />
    <path d="M3.5 13.5h4.2l1.3 2.8h6l1.3-2.8h4.2v4.8a1.5 1.5 0 0 1-1.5 1.5H5a1.5 1.5 0 0 1-1.5-1.5z" />
  </Icon>
);

export const IconServer = (p: IconProps) => (
  <Icon {...p}>
    <rect x="3" y="4" width="18" height="7" rx="1.6" />
    <rect x="3" y="13" width="18" height="7" rx="1.6" />
    <path d="M6.5 7.5h.01M6.5 16.5h.01" />
  </Icon>
);

export const IconCheck = (p: IconProps) => (
  <Icon {...p}>
    <path d="m5 12.5 4.5 4.5L19 7.5" />
  </Icon>
);
