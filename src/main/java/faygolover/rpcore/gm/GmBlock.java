package faygolover.rpcore.gm;

/**
 * Блок «ГМской штуки» (пластина, держатели, порталы RP Portals): игрокам без метки в руке не рисуется рамка
 * выделения, ведущему с меткой — рисуется контур (подсветка).
 */
public interface GmBlock {
    /** Цвет контура у ведущего, 0xRRGGBB. */
    default int markerColor() {
        return 0x40E0FF;
    }
}
