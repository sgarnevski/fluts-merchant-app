/** Full-page navigation (leaves the single-page app); its own module so tests can observe it. */
export const browser = {
  assign: (url: string) => window.location.assign(url),
}
