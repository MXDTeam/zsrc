package gui.Jieya;

/*
 * @(#)ZipConstants.java	1.18 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */


/*
 * This interface defines the constants that are used by the classes
 * which manipulate ZIP files.
 *
 * @version	1.18, 12/19/03
 * @author	David Connelly
 */
interface ZipConstants {
    /*
     * Header signatures
     */
    long LOCSIG = 0x04034b50L;	// "PK\003\004"
    long EXTSIG = 0x08074b50L;	// "PK\007\008"
    long CENSIG = 0x02014b50L;	// "PK\001\002"
    long ENDSIG = 0x06054b50L;	// "PK\005\006"

    /*
     * Header sizes in bytes (including signatures)
     */
    int LOCHDR = 30;	// LOC header size
    int EXTHDR = 16;	// EXT header size
    int CENHDR = 46;	// CEN header size
    int ENDHDR = 22;	// END header size

    /*
     * Local file (LOC) header field offsets
     */
    int LOCVER = 4;	// version needed to extract
    int LOCFLG = 6;	// general purpose bit flag
    int LOCHOW = 8;	// compression method
    int LOCTIM = 10;	// modification time
    int LOCCRC = 14;	// uncompressed file crc-32 value
    int LOCSIZ = 18;	// compressed size
    int LOCLEN = 22;	// uncompressed size
    int LOCNAM = 26;	// filename length
    int LOCEXT = 28;	// extra field length

    /*
     * Extra local (EXT) header field offsets
     */
    int EXTCRC = 4;	// uncompressed file crc-32 value
    int EXTSIZ = 8;	// compressed size
    int EXTLEN = 12;	// uncompressed size

    /*
     * Central directory (CEN) header field offsets
     */
    int CENVEM = 4;	// version made by
    int CENVER = 6;	// version needed to extract
    int CENFLG = 8;	// encrypt, decrypt flags
    int CENHOW = 10;	// compression method
    int CENTIM = 12;	// modification time
    int CENCRC = 16;	// uncompressed file crc-32 value
    int CENSIZ = 20;	// compressed size
    int CENLEN = 24;	// uncompressed size
    int CENNAM = 28;	// filename length
    int CENEXT = 30;	// extra field length
    int CENCOM = 32;	// comment length
    int CENDSK = 34;	// disk number start
    int CENATT = 36;	// internal file attributes
    int CENATX = 38;	// external file attributes
    int CENOFF = 42;	// LOC header offset

    /*
     * End of central directory (END) header field offsets
     */
    int ENDSUB = 8;	// number of entries on this disk
    int ENDTOT = 10;	// total number of entries
    int ENDSIZ = 12;	// central directory size in bytes
    int ENDOFF = 16;	// offset of first CEN header
    int ENDCOM = 20;	// zip file comment length
}
